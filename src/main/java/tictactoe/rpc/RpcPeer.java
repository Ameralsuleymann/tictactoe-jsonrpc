package tictactoe.rpc;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimaler JSON-RPC-2.0-Endpunkt ueber TCP (eine JSON-Nachricht pro Zeile).
 * Symmetrisch: BEIDE Seiten koennen Dienste anbieten (register) und fremde Dienste aufrufen (proxy).
 * - proxy(...)    = Client-Stub (dynamischer Proxy): Marshalling der Argumente, Senden, Warten, Unmarshalling
 * - handleRequest = Server-Skeleton: Unmarshalling, Reflection-Aufruf der Implementierung, Marshalling des Ergebnisses
 * Marshalling: Gson. Methoden mit Rueckgabetyp void werden als Notification (ohne id, ohne Antwort) gesendet.
 */
public class RpcPeer implements Closeable {
    private static final Gson GSON = new Gson();
    private static final long TIMEOUT_SEC = 10;

    private record Registration(Class<?> iface, Object impl) { }

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final boolean verbose;
    private final Map<String, Registration> services = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    // Eingehende Aufrufe werden in Reihenfolge auf EINEM Worker-Thread bearbeitet, damit der Reader-Thread
    // frei bleibt, um Antworten entgegenzunehmen (sonst Deadlock bei Callback waehrend eines Aufrufs).
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "rpc-worker"); t.setDaemon(true); return t; });
    private volatile Runnable onClose = () -> { };

    public RpcPeer(Socket socket, boolean verbose) throws IOException {
        this.socket = socket;
        this.verbose = verbose;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public <T> void register(String name, Class<T> iface, T impl) { services.put(name, new Registration(iface, impl)); }
    public void onClose(Runnable r) { this.onClose = r; }

    public void start() {
        Thread t = new Thread(this::readLoop, "rpc-reader");
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------- Client-Stub

    public <T> T proxy(String serviceName, Class<T> iface) {
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {   // lokal beantworten, nicht ans Netz schicken
                        return switch (method.getName()) {
                            case "equals" -> proxy == args[0];          // noetig z.B. fuer List.remove(stub)
                            case "hashCode" -> System.identityHashCode(proxy);
                            default -> "Stub[" + serviceName + "]";
                        };
                    }
                    return invokeRemote(serviceName, method, args);
                }));
    }

    private Object invokeRemote(String service, Method method, Object[] args) throws Exception {
        JsonArray params = new JsonArray();
        if (args != null) for (Object a : args) params.add(GSON.toJsonTree(a));   // Marshalling

        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", service + "." + method.getName());
        req.add("params", params);

        if (method.getReturnType() == void.class) {                                // Notification
            send(req);
            return null;
        }
        long id = ids.incrementAndGet();
        req.addProperty("id", id);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            send(req);
            JsonObject resp = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (resp.has("error")) throw rebuildException(method, resp.getAsJsonObject("error"));
            return GSON.fromJson(resp.get("result"), method.getGenericReturnType()); // Unmarshalling
        } catch (TimeoutException e) {
            throw new RpcException("Timeout bei " + method.getName());
        } catch (ExecutionException e) {
            throw new RpcException("Verbindung verloren: " + e.getCause().getMessage());
        } finally {
            pending.remove(id);
        }
    }

    /** Remote-Exception beim Aufrufer neu erzeugen, falls sie in der Methodensignatur deklariert ist. */
    private Exception rebuildException(Method method, JsonObject err) {
        String message = err.has("message") && !err.get("message").isJsonNull() ? err.get("message").getAsString() : "";
        String cls = err.has("data") ? err.getAsJsonObject("data").get("exception").getAsString() : "?";
        for (Class<?> declared : method.getExceptionTypes()) {
            if (declared.getName().equals(cls)) {
                try { return (Exception) declared.getConstructor(String.class).newInstance(message); }
                catch (ReflectiveOperationException ignored) { }
            }
        }
        return new RpcException("Remote-Fehler " + cls + ": " + message);
    }

    // ---------------------------------------------------------------- Server-Skeleton

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (verbose) System.out.println("[RPC <-] " + line);
                JsonObject msg = JsonParser.parseString(line).getAsJsonObject();
                if (msg.has("method")) {
                    worker.submit(() -> handleRequest(msg));
                } else {
                    CompletableFuture<JsonObject> f = pending.remove(msg.get("id").getAsLong());
                    if (f != null) f.complete(msg);
                }
            }
        } catch (IOException | RuntimeException e) {
            if (verbose) System.out.println("[RPC] Verbindung beendet: " + e.getMessage());
        } finally {
            RpcException lost = new RpcException("Verbindung geschlossen");
            pending.values().forEach(f -> f.completeExceptionally(lost));
            onClose.run();
            worker.shutdown();
        }
    }

    private void handleRequest(JsonObject msg) {
        JsonElement id = msg.get("id");   // null bei Notification
        try {
            String[] parts = msg.get("method").getAsString().split("\\.", 2);
            Registration reg = services.get(parts[0]);
            if (reg == null) throw new RpcException("Unbekannter Dienst: " + parts[0]);
            JsonArray params = msg.has("params") ? msg.getAsJsonArray("params") : new JsonArray();
            Method m = findMethod(reg.iface, parts[1], params.size());
            Type[] types = m.getGenericParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < args.length; i++) args[i] = GSON.fromJson(params.get(i), types[i]); // Unmarshalling
            Object result = m.invoke(reg.impl, args);
            if (id != null) {
                JsonObject resp = base(id);
                resp.add("result", GSON.toJsonTree(result, m.getGenericReturnType()));            // Marshalling
                send(resp);
            }
        } catch (InvocationTargetException e) {
            replyError(id, e.getCause());
        } catch (Exception e) {
            replyError(id, e);
        }
    }

    private static Method findMethod(Class<?> iface, String name, int argc) throws NoSuchMethodException {
        for (Method m : iface.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == argc) return m;
        throw new NoSuchMethodException(name + "/" + argc);
    }

    private void replyError(JsonElement id, Throwable t) {
        if (id == null) return;
        JsonObject err = new JsonObject();
        err.addProperty("code", -32000);
        err.addProperty("message", t.getMessage());
        JsonObject data = new JsonObject();
        data.addProperty("exception", t.getClass().getName());
        err.add("data", data);
        JsonObject resp = base(id);
        resp.add("error", err);
        send(resp);
    }

    private static JsonObject base(JsonElement id) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", id);
        return o;
    }

    private void send(JsonObject obj) {
        String line = GSON.toJson(obj);
        if (verbose) System.out.println("[RPC ->] " + line);
        synchronized (out) {
            out.println(line);
            if (out.checkError()) throw new RpcException("Senden fehlgeschlagen (Verbindung weg)");
        }
    }

    @Override public void close() throws IOException { socket.close(); }
}
