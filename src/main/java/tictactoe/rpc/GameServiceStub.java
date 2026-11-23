package tictactoe.rpc;

import javax.jms.Destination;
import javax.jms.JMSException;
import tictactoe.api.GameService;
import tictactoe.model.Move;
import tictactoe.model.Player;

/**
 * Client-Stub für {@link GameService}: implementiert dasselbe Interface wie {@link tictactoe.game.Game}, schickt jeden
 * Aufruf aber als Nachricht in die Queue {@value JmsPeer#REQUESTS} und wartet nicht (fire-and-forget).
 *
 * <p>Jede Anfrage trägt die eigene Antwort-Queue ({@code JMSReplyTo}) und eine Correlation-ID ({@code Alice-3}).
 * Scheitert sie, kommt die Fehlermeldung später über diese Queue mit derselben Correlation-ID zurück.
 */
public class GameServiceStub implements GameService {
    private final JmsPeer peer;
    private final Destination requests;
    private final Destination replyQueue;
    private int nextId;
    /** Verzögerung, die der Broker auf jede eigene Nachricht anwendet (Befehl {@code lag}); 0 = sofort. */
    private long lagMs;

    /**
     * Erzeugt den Stub.
     *
     * @param peer       Verbindung zum Broker
     * @param replyQueue eigene Antwort-Queue für Fehlermeldungen
     * @throws JMSException bei Fehlern des Brokers
     */
    public GameServiceStub(JmsPeer peer, Destination replyQueue) throws JMSException {
        this.peer = peer;
        this.requests = peer.queue(JmsPeer.REQUESTS);
        this.replyQueue = replyQueue;
    }

    /**
     * Simuliert eine langsame Leitung: Der Broker stellt ab jetzt jede eigene Nachricht verzögert zu.
     *
     * @param ms Verzögerung in Millisekunden, 0 = aus
     */
    public void setLag(long ms) { lagMs = ms; }

    @Override public void join(Player player) { send("join", player); }

    @Override public void makeMove(Move move) { send("makeMove", move); }

    @Override public void reset() { send("reset", null); }

    @Override public void leave(Player player) { send("leave", player); }

    /** Marshalling und Senden; kehrt sofort zurück. */
    private void send(String method, Object body) {
        try {
            peer.send(requests, method, body, replyQueue, peer.name() + "-" + (++nextId), lagMs);
        } catch (JMSException e) {
            throw new RpcException("Senden fehlgeschlagen: " + e.getMessage());
        }
    }
}
