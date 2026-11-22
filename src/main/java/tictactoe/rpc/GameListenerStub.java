package tictactoe.rpc;

import tictactoe.api.GameListener;
import tictactoe.model.GameState;

/**
 * Server-Stub für {@link GameListener}: implementiert dasselbe Interface wie {@link tictactoe.ui.ConsoleListener}.
 *
 * <p>{@link tictactoe.game.Game} meldet ihn wie einen lokalen Listener an. Der Stub wandelt den Zustand in JSON um
 * (Marshalling) und schickt ihn als Notification an seinen Client; der Server wartet auf keine Antwort.
 */
public class GameListenerStub implements GameListener {
    private final RpcPeer peer;

    /**
     * Erzeugt den Stub.
     *
     * @param peer Verbindung zu dem Client, der benachrichtigt werden soll
     */
    public GameListenerStub(RpcPeer peer) { this.peer = peer; }

    @Override
    public void onStateChanged(GameState state) {
        peer.sendNotification("onStateChanged", RpcPeer.GSON.toJsonTree(state));
    }
}
