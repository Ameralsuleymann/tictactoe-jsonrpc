package tictactoe.rpc;

import com.google.gson.JsonElement;
import tictactoe.api.GameException;
import tictactoe.api.GameService;
import tictactoe.model.Move;
import tictactoe.model.Player;

/**
 * Server-Skeleton für {@link GameService}: nimmt die Requests eines Clients entgegen, wandelt das JSON-Argument in das
 * passende DTO um (Unmarshalling) und ruft die echte Implementierung auf, die
 * {@link tictactoe.server.GameSession} dieses Clients.
 *
 * <p>Alle Methoden sind {@code void}, das Ergebnis ist also immer {@code null}. Eine {@link GameException} schickt der
 * {@link RpcPeer} als JSON-RPC-{@code error} zurück.
 */
public class GameServiceSkeleton implements Skeleton {
    private final GameService game;

    /**
     * Erzeugt das Skeleton.
     *
     * @param game echte Implementierung, an die die Aufrufe gehen
     */
    public GameServiceSkeleton(GameService game) { this.game = game; }

    @Override
    public JsonElement invoke(String method, JsonElement param) throws GameException {
        switch (method) {
            case "join" -> game.join(RpcPeer.GSON.fromJson(param, Player.class));
            case "makeMove" -> game.makeMove(RpcPeer.GSON.fromJson(param, Move.class));
            case "reset" -> game.reset();
            default -> throw new RpcException("Unbekannte Methode: " + method);
        }
        return null;
    }
}
