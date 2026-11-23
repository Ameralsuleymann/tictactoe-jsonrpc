package tictactoe.client;

import java.util.Scanner;
import javax.jms.Destination;
import javax.jms.JMSException;
import tictactoe.api.GameService;
import tictactoe.model.Move;
import tictactoe.model.Player;
import tictactoe.rpc.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/**
 * Startklasse des Thin Clients: nur Eingabe und Anzeige, keine Spielregeln.
 *
 * <p>Client &rarr; Server: Aufrufe über den {@link GameServiceStub} in die Queue {@value JmsPeer#REQUESTS}, ohne zu
 * warten. Server &rarr; Client: Zustand über das Topic {@value JmsPeer#STATE}, Fehler über die eigene temporäre
 * Antwort-Queue; beides kommt über das {@link GameListenerSkeleton} beim {@link ConsoleListener} an.
 *
 * <p>Start: {@code GameClient [broker-host] [name]}; {@code broker-host} ist die Vorgabe bei der Abfrage der Broker-IP.
 */
public class GameClient {
    /**
     * Verbindet sich mit dem Broker, tritt dem Spiel bei und liest dann Befehle von der Konsole.
     *
     * @param args optional {@code [broker-host] [name]}
     * @throws JMSException bei Fehlern des Brokers nach dem Verbindungsaufbau
     */
    public static void main(String[] args) throws JMSException {
        Scanner in = new Scanner(System.in);
        String broker = ask(in, "Broker-IP", args.length > 0 ? args[0] : "localhost");
        String name = args.length > 1 ? args[1] : ask(in, "Dein Name", "Spieler");

        JmsPeer peer;
        try {
            peer = new JmsPeer(broker, name);
        } catch (JMSException e) {
            System.out.println("Keine Verbindung zum Broker " + broker + " (" + e.getMessage() + ")");
            return;
        }
        Destination replyQueue = peer.temporaryQueue();
        GameListenerSkeleton skeleton = new GameListenerSkeleton(new ConsoleListener());
        peer.listen(peer.topic(JmsPeer.STATE), skeleton);   // Zustand: an alle
        peer.listen(replyQueue, skeleton);                  // Fehler: nur an mich
        GameServiceStub stub = new GameServiceStub(peer, replyQueue);
        GameService game = stub;

        Player me = new Player(name);
        game.join(me);
        System.out.println(MoveInput.HELP + " | lag <ms> = eigene Nachrichten verzögern");

        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("q")) break;
            try {
                if (line.equalsIgnoreCase("neu")) {
                    game.reset();
                } else if (line.toLowerCase().startsWith("lag")) {
                    String digits = line.replaceAll("\\D", "");
                    long ms = digits.isEmpty() ? 0 : Long.parseLong(digits);
                    stub.setLag(ms);
                    System.out.println(ms > 0 ? "Der Broker verzögert ab jetzt jede eigene Nachricht um " + ms + " ms."
                                              : "Keine Verzögerung mehr.");
                } else {
                    int[] rc = MoveInput.parse(line);
                    if (rc == null) System.out.println("Ungültige Eingabe. " + MoveInput.HELP);
                    else game.makeMove(new Move(me, rc[0], rc[1]));
                }
            } catch (RpcException e) {
                System.out.println("RPC-Fehler: " + e.getMessage());
            }
        }
        game.leave(me);
        peer.close();
    }

    /** Fragt einen Wert auf der Konsole ab; bei leerer Eingabe gilt der Vorgabewert. */
    private static String ask(Scanner in, String question, String def) {
        System.out.print(question + " [Enter = " + def + "]: ");
        String answer = in.hasNextLine() ? in.nextLine().trim() : "";
        return answer.isEmpty() ? def : answer;
    }
}
