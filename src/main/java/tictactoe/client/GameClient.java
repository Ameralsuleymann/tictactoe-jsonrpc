package tictactoe.client;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import tictactoe.api.*;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.rpc.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/**
 * Thin Client: keine Spiellogik, nur Anzeige (ConsoleListener als Callback) und Eingabe.
 * Start: java -cp ... tictactoe.client.GameClient [host] [port] [name]
 * Die Server-IP wird beim Start abgefragt (Enter = host aus den Argumenten bzw. localhost), der Name nur ohne Argument.
 */
public class GameClient {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final String HELP =
            "Befehle: 'zeile spalte' (z.B. 1 1 oder 11) | neu = Neustart / neue Runde | bench | q = beenden";

    private static volatile boolean quitting;   // eigenes Beenden ist kein "Verbindung verloren"

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
                if (MoveInput.isRestart(line)) game.reset();
                else if (line.equalsIgnoreCase("bench")) benchmark(game);
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

    private static void quit(RpcPeer peer) throws IOException {
        quitting = true;
        peer.close();      // Server merkt den Abbruch und gibt den Platz frei
        System.exit(0);
    }

    private static String ask(Scanner in, String question, String def) {
        System.out.print(question + " [Enter = " + def + "]: ");
        String s = in.hasNextLine() ? in.nextLine().trim() : "";
        return s.isEmpty() ? def : s;
    }

    /** Fragt die Server-Adresse ab ("ip" oder "ip:port") und verbindet. Bei Fehler erneut fragen; null = Abbruch. */
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

    private static String reason(IOException e, int port) {
        if (e instanceof UnknownHostException) return "Adresse unbekannt";
        if (e instanceof SocketTimeoutException) return "keine Antwort (IP falsch oder Firewall blockiert)";
        if (e instanceof ConnectException && String.valueOf(e.getMessage()).contains("refused"))
            return "abgelehnt (laeuft der Server auf Port " + port + "?)";
        return e.getMessage();
    }

    /** Vergleich: lokaler Aufruf (Game im selben Prozess) vs. entfernter RPC-Aufruf, jeweils getState(). */
    private static void benchmark(GameService remote) {
        GameService local = new Game();
        int n = 2000;
        measure(local, 500);  measure(remote, 100);                // Warm-up (JIT)
        double l = measure(local, n), r = measure(remote, n);
        System.out.printf("Lokal : %.3f us/Aufruf%nRemote: %.3f us/Aufruf%nFaktor : %.0fx langsamer%n", l, r, r / l);
    }

    private static double measure(GameService s, int n) {
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) s.getState();
        return (System.nanoTime() - t) / 1000.0 / n;
    }
}
