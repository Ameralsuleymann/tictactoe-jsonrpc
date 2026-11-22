package tictactoe.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import tictactoe.api.GameException;
import tictactoe.api.GameService;
import tictactoe.model.Move;
import tictactoe.model.Player;
import tictactoe.rpc.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/**
 * Startklasse des Thin Clients: nur Eingabe und Anzeige, keine Spielregeln.
 *
 * <p>Client &rarr; Server: Aufrufe über den {@link GameServiceStub}. Server &rarr; Client: Der Server ruft über das
 * {@link GameListenerSkeleton} den {@link ConsoleListener} auf. Beides läuft über eine TCP-Verbindung ({@link RpcPeer}).
 *
 * <p>Start: {@code GameClient [host] [port] [name]}; {@code host} ist die Vorgabe bei der Abfrage der Server-IP.
 */
public class GameClient {
    /**
     * Verbindet sich mit dem Server, tritt dem Spiel bei und liest dann Züge und Befehle von der Konsole.
     *
     * @param args optional {@code [host] [port] [name]}
     * @throws IOException bei Ein-/Ausgabefehlern der Verbindung
     */
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        String host = ask(in, "Server-IP", args.length > 0 ? args[0] : "localhost");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String name = args.length > 2 ? args[2] : ask(in, "Dein Name", "Spieler");

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
        } catch (IOException e) {
            System.out.println("Keine Verbindung zu " + host + ":" + port + " (" + e.getMessage() + ")");
            return;
        }
        RpcPeer peer = new RpcPeer(socket, Boolean.getBoolean("rpc.verbose"));
        peer.setSkeleton(new GameListenerSkeleton(new ConsoleListener()));   // Server -> Client
        peer.onClose(() -> {
            System.out.println("Verbindung zum Server beendet.");
            System.exit(0);
        });
        peer.start();
        GameService game = new GameServiceStub(peer);                       // Client -> Server

        Player me = new Player(name);
        try {
            game.join(me);
        } catch (GameException e) {
            System.out.println("Beitritt abgelehnt: " + e.getMessage());
            peer.close();
            return;
        }
        System.out.println(MoveInput.HELP);

        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("q")) break;
            try {
                if (line.equalsIgnoreCase("neu")) {
                    game.reset();
                } else {
                    int[] rc = MoveInput.parse(line);
                    if (rc == null) System.out.println("Ungültige Eingabe. " + MoveInput.HELP);
                    else game.makeMove(new Move(me, rc[0], rc[1]));
                }
            } catch (GameException e) {
                System.out.println("Fehler vom Server: " + e.getMessage());   // übertragene Exception
            } catch (RpcException e) {
                System.out.println("RPC-Fehler: " + e.getMessage());
            }
        }
        peer.close();
    }

    /** Fragt einen Wert auf der Konsole ab; bei leerer Eingabe gilt der Vorgabewert. */
    private static String ask(Scanner in, String question, String def) {
        System.out.print(question + " [Enter = " + def + "]: ");
        String answer = in.hasNextLine() ? in.nextLine().trim() : "";
        return answer.isEmpty() ? def : answer;
    }
}
