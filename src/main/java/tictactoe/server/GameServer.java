package tictactoe.server;

import java.util.Scanner;
import javax.jms.JMSException;
import tictactoe.clock.VectorClock;
import tictactoe.game.Game;
import tictactoe.rpc.*;
import tictactoe.ui.ConsoleListener;

/**
 * Startklasse des Servers: verbindet sich mit dem Broker und verarbeitet die Nachrichten der Queue
 * {@value JmsPeer#REQUESTS}.
 *
 * <p>Am {@link Game} hängen drei Beobachter, in dieser Reihenfolge: die {@link LagCompensation} (merkt sich jeden
 * Zustand fürs Zurückspulen), ein {@link ConsoleListener} (der Server zeigt jeden neuen Zustand an) und ein
 * {@link GameListenerStub} für das Topic {@value JmsPeer#STATE} (der Zustand geht an alle Clients).
 *
 * <p>Start: {@code GameServer [broker-host]}, Standard ist {@code localhost}. Beenden mit {@code q}.
 */
public class GameServer {
    /**
     * Startet den Server.
     *
     * @param args optional {@code [broker-host]}
     * @throws JMSException wenn der Broker nicht erreichbar ist
     */
    public static void main(String[] args) throws JMSException {
        String broker = args.length > 0 ? args[0] : "localhost";
        JmsPeer peer = new JmsPeer(broker, VectorClock.SERVER);
        Game game = new Game();
        LagCompensation lagCompensation = new LagCompensation(game, peer.clock());
        game.addListener(lagCompensation);                                         // 1. Zeitleiste
        game.addListener(new ConsoleListener());                                   // 2. Anzeige auf dem Server
        game.addListener(new GameListenerStub(peer, peer.topic(JmsPeer.STATE)));   // 3. an alle Clients
        peer.listen(peer.queue(JmsPeer.REQUESTS), new GameServiceSkeleton(game, lagCompensation, peer));
        System.out.println("TicTacToe-Server ist mit dem Broker " + broker + " verbunden und wartet auf "
                + JmsPeer.REQUESTS + " (beenden mit q)");

        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) if (in.nextLine().trim().equalsIgnoreCase("q")) break;
        peer.close();
    }
}
