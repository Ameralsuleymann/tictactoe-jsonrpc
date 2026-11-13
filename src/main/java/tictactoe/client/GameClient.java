package tictactoe.client;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import tictactoe.api.*;
import tictactoe.model.*;
import tictactoe.rpc.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/**
 * Startklasse des Thin Clients der verteilten Version (Phase 2).
 *
 * <p>Der Client enthaelt keine Spiellogik, nur Anzeige und Eingabe: Er liest Befehle von der Konsole, ruft das
 * Spiel ueber einen {@link GameService}-Stub auf (Client &rarr; Server) und zeigt Zustandsaenderungen ueber einen
 * {@link ConsoleListener} an, den der Server per Callback aufruft (Server &rarr; Client). Beide Richtungen laufen
 * ueber dieselbe TCP-Verbindung und denselben {@link RpcPeer}.
 *
 * <p>Start: {@code java -cp ... tictactoe.client.GameClient [host] [port] [name]}<br>
 * Die Server-IP wird beim Start abgefragt (Enter = {@code host} aus den Argumenten bzw. {@code localhost}),
 * der Name nur, wenn er nicht als Argument angegeben ist.
 *
 * <p>Befehle: {@code zeile spalte} (z.B. {@code 1 1} oder {@code 11}), {@code neu} (Neustart bzw. neue Runde),
 * {@code bench [anzahl]} (Zeitvergleich lokal/remote, siehe {@link Benchmark}), {@code q} (beenden).
 */
public class GameClient {
    /** Timeout fuer den Verbindungsaufbau zum Server in Millisekunden. */
    private static final int CONNECT_TIMEOUT_MS = 5000;
    /** Hilfetext mit allen Befehlen; wird nach dem Beitritt und bei ungueltiger Eingabe angezeigt. */
    private static final String HELP =
            "Befehle: 'zeile spalte' (z.B. 1 1 oder 11) | neu = Neustart / neue Runde | bench [anzahl] = Zeitvergleich | q = beenden";

    /**
     * Wird vor dem gewollten Beenden gesetzt, damit der {@code onClose}-Handler des {@link RpcPeer} das Schliessen
     * der Verbindung nicht als Verbindungsverlust meldet.
     */
    private static volatile boolean quitting;   // eigenes Beenden ist kein "Verbindung verloren"

    /** Standardkonstruktor; der Client wird ueber {@link #main(String[])} gestartet. */
    public GameClient() { }

    /**
     * Startet den Client: Name und Server-Adresse abfragen, verbinden, den {@link ConsoleListener} als Dienst
     * {@code "listener"} registrieren, dem Spiel beitreten und anschliessend die Eingabeschleife ausfuehren.
     *
     * <p>{@link GameException} (fachlicher Fehler, vom Server uebertragen) und {@link RpcException} (technischer
     * Fehler) werden gefangen und angezeigt, damit der Client weiterlaeuft.
     *
     * @param args optional {@code [host] [port] [name]}; {@code host} ist nur die Vorgabe fuer die IP-Abfrage
     * @throws Exception bei nicht behandelten Ein-/Ausgabefehlern
     */
    public static void main(String[] args) throws Exception {
        String defaultHost = args.length > 0 ? args[0] : "localhost";
        int defaultPort = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        Scanner in = new Scanner(System.in);
        String name = args.length > 2 ? args[2] : ask(in, "Dein Name", "Spieler");

        Socket socket = connect(in, defaultHost, defaultPort);
        if (socket == null) return;

        RpcPeer peer = new RpcPeer(socket, Boolean.getBoolean("rpc.verbose"));
        peer.register("listener", GameListener.class, new ConsoleListener()); // Server darf uns aufrufen
        peer.onClose(() -> {
            if (quitting) return;
            System.out.println("Verbindung zum Server verloren.");
            System.exit(1);
        });
        peer.start();

        GameService game = peer.proxy("game", GameService.class);             // Stub: Client -> Server
        Player me;
        try { me = game.join(new Player(name)); }
        catch (GameException e) { System.out.println("Beitritt abgelehnt: " + e.getMessage()); quit(peer); return; }
        System.out.println("Du bist " + me);
        System.out.println(HELP);

        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (MoveInput.isQuit(line)) break;
            try {
                int benchCalls = Benchmark.parseCalls(line);
                if (MoveInput.isRestart(line)) game.reset();
                else if (benchCalls > 0) Benchmark.run(game, peer, socket.getInetAddress(), benchCalls);
                else {
                    int[] rc = MoveInput.parse(line);
                    if (rc == null) System.out.println("Ungueltige Eingabe. " + HELP);
                    else game.makeMove(new Move(me, rc[0], rc[1]));
                }
            } catch (GameException e) {
                System.out.println("Server-Fehler (GameException): " + e.getMessage());   // Exception ueber RPC
            } catch (RpcException e) {
                System.out.println("RPC-Fehler: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Fehler: " + e);
            }
        }
        quit(peer);
    }

    /**
     * Beendet den Client geordnet: Verbindung schliessen (der Server gibt daraufhin den Platz frei) und JVM beenden.
     *
     * @param peer der zu schliessende RPC-Endpunkt
     * @throws IOException wenn das Schliessen des Sockets fehlschlaegt
     */
    private static void quit(RpcPeer peer) throws IOException {
        quitting = true;
        peer.close();      // Server merkt den Abbruch und gibt den Platz frei
        System.exit(0);
    }

    /**
     * Fragt einen Wert auf der Konsole ab und liefert bei leerer Eingabe den Vorgabewert.
     *
     * @param in       Konsoleneingabe
     * @param question angezeigte Frage
     * @param def      Vorgabewert bei leerer Eingabe
     * @return die eingegebene (getrimmte) Antwort oder {@code def}
     */
    private static String ask(Scanner in, String question, String def) {
        System.out.print(question + " [Enter = " + def + "]: ");
        String s = in.hasNextLine() ? in.nextLine().trim() : "";
        return s.isEmpty() ? def : s;
    }

    /**
     * Fragt die Server-Adresse ab ({@code ip} oder {@code ip:port}) und baut die TCP-Verbindung auf.
     * Schlaegt der Verbindungsaufbau fehl, wird der Grund angezeigt und erneut gefragt.
     *
     * @param in          Konsoleneingabe
     * @param defaultHost Host bei leerer Eingabe
     * @param defaultPort Port, falls in der Eingabe keiner angegeben ist
     * @return der verbundene Socket oder {@code null}, wenn mit {@code q} abgebrochen wird oder die Eingabe endet
     */
    private static Socket connect(Scanner in, String defaultHost, int defaultPort) {
        while (true) {
            System.out.print("Server-IP [Enter = " + defaultHost + "]: ");
            if (!in.hasNextLine()) return null;
            String host = in.nextLine().trim();
            if (MoveInput.isQuit(host)) return null;
            if (host.isEmpty()) host = defaultHost;

            int port = defaultPort;
            int colon = host.indexOf(':');
            if (colon > 0 && colon == host.lastIndexOf(':')) {             // "host:port" (IPv6 hat mehrere ':')
                try { port = Integer.parseInt(host.substring(colon + 1)); } catch (NumberFormatException e) { port = -1; }
                host = host.substring(0, colon);
            }
            if (port < 1 || port > 65535) { System.out.println("Ungueltiger Port."); continue; }

            System.out.println("Verbinde mit " + host + ":" + port + " ...");
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                System.out.println("Verbunden.");
                return socket;
            } catch (IOException e) {
                try { socket.close(); } catch (IOException ignored) { }
                System.out.println("Keine Verbindung: " + reason(e, port) + " - IP erneut eingeben oder q");
            }
        }
    }

    /**
     * Uebersetzt eine Exception beim Verbindungsaufbau in einen verstaendlichen Grund.
     *
     * @param e    die beim Verbinden aufgetretene Exception
     * @param port der verwendete Port (fuer den Hinweis bei abgelehnter Verbindung)
     * @return kurze Fehlerbeschreibung fuer die Konsole
     */
    private static String reason(IOException e, int port) {
        if (e instanceof UnknownHostException) return "Adresse unbekannt";
        if (e instanceof SocketTimeoutException) return "keine Antwort (IP falsch oder Firewall blockiert)";
        if (e instanceof ConnectException && String.valueOf(e.getMessage()).contains("refused"))
            return "abgelehnt (laeuft der Server auf Port " + port + "?)";
        return e.getMessage();
    }
}
