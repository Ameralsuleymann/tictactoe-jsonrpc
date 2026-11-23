package tictactoe.rpc;

import javax.jms.Destination;
import javax.jms.JMSException;
import tictactoe.api.GameListener;
import tictactoe.model.GameState;

/**
 * Server-Stub für {@link GameListener}: implementiert dasselbe Interface wie {@link tictactoe.ui.ConsoleListener},
 * schickt jeden Aufruf aber als Nachricht an eine Destination.
 *
 * <p>Der Server nutzt zwei Arten: einen Stub für das Topic {@value JmsPeer#STATE} (beim {@code Game} angemeldet, der
 * Zustand geht an alle Clients) und für jede gescheiterte Anfrage einen Stub für die Antwort-Queue des Absenders
 * ({@code onError} nur an ihn, mit der Correlation-ID der Anfrage).
 */
public class GameListenerStub implements GameListener {
    private final JmsPeer peer;
    private final Destination to;
    private final String correlationId;

    /**
     * Stub für das Topic: der Zustand geht an alle Clients.
     *
     * @param peer Verbindung zum Broker
     * @param to   das Topic
     */
    public GameListenerStub(JmsPeer peer, Destination to) { this(peer, to, null); }

    /**
     * Stub für die Antwort-Queue eines Clients.
     *
     * @param peer          Verbindung zum Broker
     * @param to            Antwort-Queue des Absenders
     * @param correlationId Kennung der Anfrage, auf die sich die Nachricht bezieht
     */
    public GameListenerStub(JmsPeer peer, Destination to, String correlationId) {
        this.peer = peer;
        this.to = to;
        this.correlationId = correlationId;
    }

    @Override public void onStateChanged(GameState state) { send("onStateChanged", state); }

    @Override public void onError(String message) { send("onError", message); }

    private void send(String method, Object body) {
        try {
            peer.send(to, method, body, null, correlationId, 0);
        } catch (JMSException e) {
            System.out.println("[JMS] " + method + " nicht gesendet: " + e.getMessage());
        }
    }
}
