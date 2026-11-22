package tictactoe.rpc;

import com.google.gson.JsonObject;
import tictactoe.api.GameException;
import tictactoe.api.GameService;
import tictactoe.model.Move;
import tictactoe.model.Player;

/**
 * Client-Stub für {@link GameService}: implementiert dasselbe Interface wie {@link tictactoe.game.Game}, schickt jeden
 * Aufruf aber als JSON-RPC-Request zum Server und wartet auf die Antwort.
 *
 * <p>Der Client ({@link tictactoe.client.GameClient}) merkt davon nichts (Zugriffstransparenz). Antwortet der Server
 * mit einer {@link GameException}, erzeugt der Stub sie neu und wirft sie beim Aufrufer.
 */
public class GameServiceStub implements GameService {
    private final RpcPeer peer;

    /**
     * Erzeugt den Stub.
     *
     * @param peer Verbindung zum Server
     */
    public GameServiceStub(RpcPeer peer) { this.peer = peer; }

    @Override public void join(Player player) throws GameException { call("join", player); }

    @Override public void makeMove(Move move) throws GameException { call("makeMove", move); }

    @Override public void reset() throws GameException { call("reset", null); }

    /**
     * Marshalling des Arguments, Request senden, auf die Antwort warten und sie auswerten.
     *
     * @param method Methodenname
     * @param param  das Argument (DTO) oder {@code null}
     * @throws GameException wenn der Server mit einer GameException geantwortet hat
     * @throws RpcException  bei anderen Fehlern des Servers, Timeout oder Verbindungsverlust
     */
    private void call(String method, Object param) throws GameException {
        JsonObject response = peer.sendRequest(method, param == null ? null : RpcPeer.GSON.toJsonTree(param));
        if (!response.has("error")) return;                                  // "result": null = erfolgreich
        JsonObject error = response.getAsJsonObject("error");
        String message = error.get("message").getAsString();
        String exception = error.getAsJsonObject("data").get("exception").getAsString();
        if (exception.equals(GameException.class.getName())) throw new GameException(message);
        throw new RpcException("Fehler auf dem Server (" + exception + "): " + message);
    }
}
