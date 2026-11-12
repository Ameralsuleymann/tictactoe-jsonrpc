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
 * Minimaler JSON-RPC-2.0-Endpunkt ueber TCP, eine JSON-Nachricht pro Zeile.
 *
 * <p>Symmetrisch: BEIDE Seiten koennen Dienste anbieten ({@link #register(String, Class, Object)}) und Dienste der
 * Gegenseite aufrufen ({@link #proxy(String, Class)}). Dadurch laufen Aufrufe Client &rarr; Server und Callbacks
 * Server &rarr; Client ueber denselben Socket.
 * <ul>
 *   <li>{@link #proxy(String, Class)} erzeugt den <b>Client-Stub</b> (dynamischer Proxy): Marshalling der
 *       Argumente, Senden, Warten auf die Antwort, Unmarshalling des Ergebnisses bzw. Neuerzeugen der Exception.</li>
 *   <li>{@code handleRequest} ist das generische <b>Server-Skeleton</b>: Unmarshalling der Parameter, Aufruf der
 *       Implementierung per Reflection, Marshalling des Ergebnisses.</li>
 * </ul>
 * Marshalling und Unmarshalling uebernimmt Gson. Methoden mit Rueckgabetyp {@code void} werden als Notification
 * (ohne {@code id}, ohne Antwort) gesendet.
 *
 * <p>Threads: Der Reader-Thread ({@code rpc-reader}) liest alle eingehenden Nachrichten, ordnet Antworten ueber ihre
 * {@code id} den wartenden Aufrufen zu und uebergibt eingehende Aufrufe an einen Worker-Thread ({@code rpc-worker}).
 *
 * <p>Beispiel einer Anfrage:
 * <pre>{@code
 * {"jsonrpc":"2.0","method":"game.makeMove","params":[{"player":{"name":"Alice","symbol":"X"},"row":1,"col":1}],"id":3}
 * }</pre>
 */
public class RpcPeer implements Closeable {
    /** Gson-Instanz fuer Marshalling (Java &rarr; JSON) und Unmarshalling (JSON &rarr; Java); thread-sicher. */
    private static final Gson GSON = new Gson();
    /** Maximale Wartezeit auf eine Antwort in Sekunden; danach wird eine {@link RpcException} geworfen. */
    private static final long TIMEOUT_SEC = 10;

    /**
     * Unter einem Dienstnamen registrierte Implementierung.
     *
     * @param iface Schnittstelle, deren Methoden von der Gegenseite aufgerufen werden duerfen
     * @param impl  Objekt, das die Aufrufe ausfuehrt
     */
    private record Registration(Class<?> iface, Object impl) { }

    /** TCP-Verbindung zur Gegenseite. */
    private final Socket socket;
    /** Eingabestrom; liest eine JSON-Nachricht pro Zeile. */
    private final BufferedReader in;
    /** Ausgabestrom (autoFlush); schreibt eine JSON-Nachricht pro Zeile. */
    private final PrintWriter out;
    /** Wenn {@code true}, werden alle gesendeten und empfangenen Nachrichten auf der Konsole ausgegeben. */
    private final boolean verbose;
    /** Angebotene Dienste: Dienstname ({@code game} bzw. {@code listener}) &rarr; Registrierung. */
    private final Map<String, Registration> services = new ConcurrentHashMap<>();
    /** Offene Aufrufe: Anfrage-{@code id} &rarr; Future, das der Reader-Thread mit der Antwort erfuellt. */
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    /** Zaehler fuer eindeutige Anfrage-{@code id}s. */
    private final AtomicLong ids = new AtomicLong();
    /**
     * Einzelner Worker-Thread fuer eingehende Aufrufe.
     *
     * <p>Eingehende Aufrufe werden in Reihenfolge auf EINEM Worker-Thread bearbeitet, damit der Reader-Thread
     * frei bleibt, um Antworten entgegenzunehmen (sonst Deadlock bei Callback waehrend eines Aufrufs).
     */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "rpc-worker"); t.setDaemon(true); return t; });
    /** Wird ausgefuehrt, sobald die Verbindung endet; standardmaessig ohne Wirkung. */
    private volatile Runnable onClose = () -> { };

    /**
     * Erzeugt einen Endpunkt fuer eine bereits aufgebaute Verbindung; gelesen wird erst nach {@link #start()}.
     *
     * @param socket  verbundener Socket (Client- oder Serverseite)
     * @param verbose {@code true}, um alle JSON-Nachrichten auf der Konsole auszugeben
     * @throws IOException wenn die Datenstroeme des Sockets nicht geoeffnet werden koennen
     */
    public RpcPeer(Socket socket, boolean verbose) throws IOException {
        this.socket = socket;
        this.verbose = verbose;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /**
     * Bietet der Gegenseite ein Objekt als Dienst an.
     *
     * <p>Aufrufe {@code "<name>.<methode>"} werden an {@code impl} weitergeleitet, aber nur fuer Methoden von
     * {@code iface}.
     *
     * @param name  Dienstname, z.B. {@code "game"} auf dem Server oder {@code "listener"} auf dem Client
     * @param iface angebotene Schnittstelle
     * @param impl  Implementierung, die die Aufrufe ausfuehrt
     * @param <T>   Typ der Schnittstelle
     */
    public <T> void register(String name, Class<T> iface, T impl) { services.put(name, new Registration(iface, impl)); }

    /**
     * Legt fest, was beim Ende der Verbindung passieren soll (Server: Spieler abmelden, Client: Meldung und Ende).
     * Ein spaeterer Aufruf ersetzt den vorherigen Handler.
     *
     * @param r Aktion, die im Reader-Thread ausgefuehrt wird, sobald die Verbindung geschlossen ist
     */
    public void onClose(Runnable r) { this.onClose = r; }

    /**
     * Startet den Reader-Thread, der eingehende Nachrichten verarbeitet.
     * Erst danach werden Aufrufe beantwortet und Antworten empfangen; Dienste sollten vorher registriert sein.
     */
    public void start() {
        Thread t = new Thread(this::readLoop, "rpc-reader");
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------- Client-Stub

    /**
     * Erzeugt einen Client-Stub fuer einen Dienst der Gegenseite (Proxy-Pattern mit {@link java.lang.reflect.Proxy}).
     *
     * <p>Jeder Methodenaufruf auf dem Stub wird zu einer JSON-RPC-Nachricht {@code "<serviceName>.<methode>"}.
     * Die Methoden von {@link Object} ({@code equals}, {@code hashCode}, {@code toString}) werden lokal beantwortet,
     * damit der Stub z.B. in Listen eingefuegt und wieder entfernt werden kann.
     *
     * @param serviceName Name, unter dem die Gegenseite den Dienst registriert hat
     * @param iface       Schnittstelle des Dienstes
     * @param <T>         Typ der Schnittstelle
     * @return Stub, der {@code iface} implementiert und Aufrufe ueber das Netz weiterleitet
     */
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

    /**
     * Fuehrt einen entfernten Aufruf aus; das ist der Kern des Stubs.
     *
     * <p>Die Argumente werden mit Gson in JSON umgewandelt (Marshalling). Bei {@code void}-Methoden wird eine
     * Notification ohne {@code id} gesendet und sofort zurueckgekehrt. Sonst wird eine eindeutige {@code id} vergeben
     * und bis zu {@link #TIMEOUT_SEC} Sekunden blockierend auf die Antwort gewartet. Das Ergebnis wird in den
     * Rueckgabetyp der Methode umgewandelt (Unmarshalling).
     *
     * @param service Dienstname der Gegenseite
     * @param method  aufgerufene Schnittstellenmethode
     * @param args    Argumente des Aufrufs, {@code null} bei parameterlosen Methoden
     * @return das Ergebnis als Java-Objekt oder {@code null} bei {@code void}-Methoden
     * @throws Exception die neu erzeugte, in der Signatur deklarierte Remote-Exception
     *                   (z.B. {@link tictactoe.api.GameException}) oder eine {@link RpcException} bei Timeout,
     *                   Verbindungsverlust oder nicht deklariertem Remote-Fehler
     */
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

    /**
     * Remote-Exception beim Aufrufer neu erzeugen, falls sie in der Methodensignatur deklariert ist.
     *
     * <p>Ist die in {@code data.exception} genannte Klasse in der {@code throws}-Klausel von {@code method}
     * enthalten, wird sie ueber ihren Konstruktor mit einem {@code String}-Parameter und der Fehlermeldung erzeugt.
     * Andernfalls, oder wenn das nicht gelingt, entsteht eine {@link RpcException}.
     *
     * @param method aufgerufene Methode, deren {@code throws}-Klausel ausgewertet wird
     * @param err    das {@code error}-Objekt der JSON-RPC-Antwort
     * @return die beim Aufrufer zu werfende Exception
     */
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

    /**
     * Hauptschleife des Reader-Threads: liest Zeile fuer Zeile und unterscheidet die Nachrichten.
     * <ul>
     *   <li>mit {@code method}: eingehender Aufruf, wird an den Worker-Thread uebergeben,</li>
     *   <li>ohne {@code method}: Antwort, erfuellt das wartende Future mit derselben {@code id}.</li>
     * </ul>
     * Endet die Verbindung, werden alle offenen Aufrufe mit einer {@link RpcException} beendet, der
     * {@code onClose}-Handler ausgefuehrt und der Worker-Thread heruntergefahren.
     */
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

    /**
     * Skeleton und Dispatcher: fuehrt einen eingehenden Aufruf auf der registrierten Implementierung aus.
     *
     * <p>Aus {@code "dienst.methode"} werden die Registrierung und per Reflection die passende Methode der
     * Schnittstelle ermittelt. Die Parameter werden anhand der generischen Parametertypen aus JSON erzeugt
     * (Unmarshalling). Bei einer Anfrage mit {@code id} wird das Ergebnis als {@code result} (Marshalling) bzw. ein
     * Fehler als {@code error} zurueckgesendet; Notifications erhalten keine Antwort.
     *
     * @param msg die empfangene JSON-RPC-Anfrage
     */
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

    /**
     * Sucht eine Methode der Schnittstelle anhand von Name und Parameteranzahl.
     * JSON kennt keine Java-Typen, Ueberladungen mit gleicher Parameteranzahl werden daher nicht unterschieden.
     *
     * @param iface Schnittstelle des Dienstes
     * @param name  Methodenname
     * @param argc  Anzahl der uebergebenen Parameter
     * @return die gefundene Methode
     * @throws NoSuchMethodException wenn keine passende Methode existiert
     */
    private static Method findMethod(Class<?> iface, String name, int argc) throws NoSuchMethodException {
        for (Method m : iface.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == argc) return m;
        throw new NoSuchMethodException(name + "/" + argc);
    }

    /**
     * Sendet eine JSON-RPC-Fehlerantwort mit Meldung und dem Klassennamen der Exception in {@code data.exception}.
     * Damit kann der Aufrufer die Exception neu erzeugen. Bei Notifications ({@code id} ist {@code null}) wird nichts
     * gesendet.
     *
     * @param id ID der Anfrage oder {@code null}
     * @param t  aufgetretener Fehler
     */
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

    /**
     * Erzeugt das Grundgeruest einer Antwort mit {@code jsonrpc} und {@code id}.
     *
     * @param id ID der zugehoerigen Anfrage
     * @return neues JSON-Objekt
     */
    private static JsonObject base(JsonElement id) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", id);
        return o;
    }

    /**
     * Serialisiert eine Nachricht und schreibt sie als eine Zeile auf den Socket.
     * Synchronisiert, da Reader-, Worker- und Anwendungs-Threads gleichzeitig senden koennen.
     *
     * @param obj zu sendende JSON-RPC-Nachricht
     * @throws RpcException wenn das Schreiben fehlschlaegt (Verbindung weg)
     */
    private void send(JsonObject obj) {
        String line = GSON.toJson(obj);
        if (verbose) System.out.println("[RPC ->] " + line);
        synchronized (out) {
            out.println(line);
            if (out.checkError()) throw new RpcException("Senden fehlgeschlagen (Verbindung weg)");
        }
    }

    /**
     * Schliesst den Socket. Der Reader-Thread erkennt daraufhin das Verbindungsende, beendet offene Aufrufe und fuehrt
     * den {@code onClose}-Handler aus.
     *
     * @throws IOException wenn das Schliessen fehlschlaegt
     */
    @Override public void close() throws IOException { socket.close(); }
}
