package tictactoe.client;

import java.util.Scanner;
import java.net.Socket;
import tictactoe.api.*;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.rpc.*;
import tictactoe.ui.ConsoleListener;

/**
 * Thin Client: keine Spiellogik, nur Anzeige (ConsoleListener als Callback) und Eingabe.
 * Start: java -cp ... tictactoe.client.GameClient <host> <port> <name>
 */
public class GameClient {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String name = args.length > 2 ? args[2] : "Spieler";

        RpcPeer peer = new RpcPeer(new Socket(host, port), Boolean.getBoolean("rpc.verbose"));
        peer.register("listener", GameListener.class, new ConsoleListener()); // Server darf uns aufrufen
        peer.onClose(() -> { System.out.println("Verbindung zum Server verloren."); System.exit(1); });
        peer.start();

        GameService game = peer.proxy("game", GameService.class);             // Stub: Client -> Server
        Player me;
        try { me = game.join(new Player(name)); }
        catch (GameException e) { System.out.println("Beitritt abgelehnt: " + e.getMessage()); return; }
        System.out.println("Du bist " + me);
        System.out.println("Eingabe: 'zeile spalte' (z.B. 1 1) | reset | bench | q");

        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (line.equals("q")) break;
            try {
                if (line.equals("reset")) game.reset();
                else if (line.equals("bench")) benchmark(game);
                else {
                    String[] p = line.split("\\s+");
                    game.makeMove(new Move(me, Integer.parseInt(p[0]), Integer.parseInt(p[1])));
                }
            } catch (GameException e) {
                System.out.println("Server-Fehler (GameException): " + e.getMessage());   // Exception ueber RPC
            } catch (RpcException e) {
                System.out.println("RPC-Fehler: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ungueltige Eingabe.");
            }
        }
        peer.close();
        System.exit(0);
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
