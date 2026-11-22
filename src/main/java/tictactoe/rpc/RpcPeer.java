package tictactoe.rpc;

import com.google.gson.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Transport für JSON-RPC 2.0 über TCP: eine JSON-Nachricht pro Zeile, in beide Richtungen über denselben Socket.
 *
 * <p>Der Peer kennt keine Spielklassen. Was gesendet wird, entscheiden die Stubs; eingehende Aufrufe gibt er an das
 * {@link Skeleton} seiner Seite weiter. Es gibt zwei Arten von Aufrufen:
 * <ul>
 *   <li><b>Request</b> ({@link #sendRequest}, mit {@code id}): Der Aufrufer wartet auf die Antwort mit derselben
 *       {@code id}. So laufen alle Aufrufe Client &rarr; Server.</li>
 *   <li><b>Notification</b> ({@link #sendNotification}, ohne {@code id}): keine Antwort, der Aufrufer wartet nicht.
 *       So laufen die Callbacks Server &rarr; Client.</li>
 * </ul>
 * Ein Reader-Thread liest alle eingehenden Zeilen: Eine Antwort weckt den wartenden Aufrufer, einen Aufruf der
 * Gegenseite führt er direkt über das Skeleton aus.
 *
 * <p>Beispiele (sichtbar mit {@code -Drpc.verbose=true}):
 * <pre>
 * Request:      {"jsonrpc":"2.0","method":"makeMove","params":{"player":{"name":"Alice","symbol":null},"row":1,"col":1},"id":2}
 * Antwort:      {"jsonrpc":"2.0","id":2,"result":null}
 * Fehler:       {"jsonrpc":"2.0","id":2,"error":{"code":-32000,"message":"Feld ist bereits belegt","data":{"exception":"tictactoe.api.GameException"}}}
 * Notification: {"jsonrpc":"2.0","method":"onStateChanged","params":{"board":{...},"players":[...],"current":"O","status":"RUNNING"}}
 * </pre>
 * Weil jede Methode höchstens ein Argument hat, ist {@code params} direkt dieses eine Argument.
 */
public class RpcPeer implements Closeable {
    /** Marshalling Java &harr; JSON für Stubs und Skeletons; {@code serializeNulls}, damit {@code "result":null} gesendet wird. */
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    /** Maximale Wartezeit auf eine Antwort in Sekunden. */
    private static final long TIMEOUT_SEC = 10;

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    /** Alle Nachrichten auf der Konsole ausgeben ({@code -Drpc.verbose=true}). */
    private final boolean verbose;
    /** Offene Requests: {@code id} &rarr; Future, das der Reader-Thread mit der Antwort erfüllt. */
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();
    /** Führt eingehende Aufrufe aus; vor {@link #start()} setzen. */
    private Skeleton skeleton;
    /** Wird ausgeführt, wenn die Verbindung endet; vor {@link #start()} setzen. */
    private Runnable onClose = () -> { };

    /**
     * Erzeugt einen Peer für eine aufgebaute Verbindung; gelesen wird erst nach {@link #start()}.
     *
     * @param socket  verbundener Socket
     * @param verbose {@code true}, um alle gesendeten und empfangenen Nachrichten auszugeben
     * @throws IOException wenn die Datenströme des Sockets nicht geöffnet werden können
     */
    public RpcPeer(Socket socket, boolean verbose) throws IOException {
        this.socket = socket;
        this.verbose = verbose;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /**
     * Legt fest, wer eingehende Aufrufe ausführt (Server: {@link GameServiceSkeleton}, Client:
     * {@link GameListenerSkeleton}).
     *
     * @param skeleton Skeleton dieser Seite
     */
    public void setSkeleton(Skeleton skeleton) { this.skeleton = skeleton; }

    /**
     * Legt fest, was beim Verbindungsende passiert (Server: Spieler abmelden, Client: beenden).
     *
     * @param onClose Aktion, die im Reader-Thread ausgeführt wird
     */
    public void onClose(Runnable onClose) { this.onClose = onClose; }

    /** Startet den Reader-Thread. */
    public void start() {
        Thread reader = new Thread(this::readLoop, "rpc-reader");
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Sendet einen Request und wartet auf die Antwort.
     *
     * @param method Methodenname
     * @param param  das Argument als JSON oder {@code null} bei Methoden ohne Argument
     * @return die Antwort; sie enthält {@code result} oder {@code error}
     * @throws RpcException wenn keine Antwort kommt oder die Verbindung endet
     */
    public JsonObject sendRequest(String method, JsonElement param) {
        long id = nextId.incrementAndGet();
        CompletableFuture<JsonObject> answer = new CompletableFuture<>();
        pending.put(id, answer);
        JsonObject request = message(method, param);
        request.addProperty("id", id);
        try {
            send(request);
            return answer.get(TIMEOUT_SEC, TimeUnit.SECONDS);       // warten, bis der Reader-Thread die Antwort liefert
        } catch (TimeoutException e) {
            throw new RpcException("Keine Antwort auf " + method + " nach " + TIMEOUT_SEC + " s");
        } catch (ExecutionException | InterruptedException e) {
            throw new RpcException("Verbindung verloren");
        } finally {
            pending.remove(id);
        }
    }

    /**
     * Sendet eine Notification (fire-and-forget): keine {@code id}, keine Antwort, kein Warten.
     *
     * @param method Methodenname
     * @param param  das Argument als JSON
     */
    public void sendNotification(String method, JsonElement param) { send(message(method, param)); }

    /**
     * Schließt den Socket; der Reader-Thread bemerkt das Verbindungsende und führt {@code onClose} aus.
     *
     * @throws IOException wenn das Schließen fehlschlägt
     */
    @Override public void close() throws IOException { socket.close(); }

    /** Reader-Thread: liest Zeile für Zeile, bis die Verbindung endet. */
    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (verbose) System.out.println("[RPC <-] " + line);
                JsonObject msg = JsonParser.parseString(line).getAsJsonObject();
                if (msg.has("method")) {
                    handleCall(msg);                                    // Aufruf der Gegenseite
                } else {
                    CompletableFuture<JsonObject> answer = pending.get(msg.get("id").getAsLong());
                    if (answer != null) answer.complete(msg);           // Antwort: wartenden Aufrufer wecken
                }
            }
        } catch (IOException | RuntimeException e) {
            if (verbose) System.out.println("[RPC] Verbindung beendet: " + e.getMessage());
        } finally {
            pending.values().forEach(f -> f.completeExceptionally(new RpcException("Verbindung geschlossen")));
            onClose.run();
        }
    }

    /** Führt einen eingehenden Aufruf über das Skeleton aus; nur ein Request (mit {@code id}) bekommt eine Antwort. */
    private void handleCall(JsonObject msg) {
        String method = msg.get("method").getAsString();
        JsonElement id = msg.get("id");                                 // null bei einer Notification
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        try {
            response.add("result", skeleton.invoke(method, msg.get("params")));
        } catch (Exception e) {
            if (verbose) System.out.println("[RPC] Fehler in " + method + ": " + e);
            response.add("error", error(e));
        }
        if (id != null) send(response);
    }

    /** Baut das JSON-RPC-{@code error}-Objekt; der Klassenname erlaubt dem Stub, die Exception neu zu erzeugen. */
    private static JsonObject error(Exception e) {
        JsonObject data = new JsonObject();
        data.addProperty("exception", e.getClass().getName());
        JsonObject error = new JsonObject();
        error.addProperty("code", -32000);
        error.addProperty("message", Objects.requireNonNullElse(e.getMessage(), e.toString()));
        error.add("data", data);
        return error;
    }

    /** Grundgerüst eines Aufrufs: {@code jsonrpc}, {@code method} und, falls vorhanden, {@code params}. */
    private static JsonObject message(String method, JsonElement param) {
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        if (param != null) msg.add("params", param);
        return msg;
    }

    /** Schreibt eine Nachricht als eine Zeile; synchronized, weil mehrere Threads über denselben Socket senden. */
    private void send(JsonObject msg) {
        String line = GSON.toJson(msg);
        if (verbose) System.out.println("[RPC ->] " + line);
        synchronized (out) { out.println(line); }
    }
}
