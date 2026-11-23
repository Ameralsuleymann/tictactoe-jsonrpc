package tictactoe.rpc;

import tictactoe.api.GameException;
import tictactoe.game.Game;
import tictactoe.model.Move;
import tictactoe.model.Player;
import tictactoe.server.LagCompensation;

/**
 * Server-Skeleton für {@link tictactoe.api.GameService}: nimmt die Nachrichten aus der Queue
 * {@value JmsPeer#REQUESTS}, wandelt den JSON-Body in das passende DTO um (Unmarshalling) und ruft das Spiel auf.
 *
 * <p>Züge gehen über die {@link LagCompensation}, die mit dem Zeitstempel der Nachricht entscheidet, ob zurückgespult
 * wird. Scheitert ein Aufruf mit einer {@link GameException}, geht die Meldung als {@code onError} nur an den Absender
 * (seine {@code JMSReplyTo}-Queue, mit derselben Correlation-ID).
 */
public class GameServiceSkeleton implements Skeleton {
    private final Game game;
    private final LagCompensation lagCompensation;
    private final JmsPeer peer;

    /**
     * Erzeugt das Skeleton.
     *
     * @param game            das Spiel
     * @param lagCompensation prüft und spult Züge zurück
     * @param peer            Verbindung zum Broker, für Fehlermeldungen an den Absender
     */
    public GameServiceSkeleton(Game game, LagCompensation lagCompensation, JmsPeer peer) {
        this.game = game;
        this.lagCompensation = lagCompensation;
        this.peer = peer;
    }

    @Override
    public void invoke(Call call) {
        try {
            switch (call.method()) {
                case "join" -> game.join(JmsPeer.GSON.fromJson(call.body(), Player.class));
                case "makeMove" -> lagCompensation.makeMove(JmsPeer.GSON.fromJson(call.body(), Move.class), call.stamp());
                case "reset" -> game.reset();
                case "leave" -> game.leave(JmsPeer.GSON.fromJson(call.body(), Player.class));
                default -> throw new GameException("Unbekannte Methode: " + call.method());
            }
        } catch (GameException e) {
            if (call.replyTo() != null)
                new GameListenerStub(peer, call.replyTo(), call.correlationId()).onError(e.getMessage());
        }
    }
}
