package tictactoe.rpc;

import com.google.gson.JsonElement;
import tictactoe.api.GameListener;
import tictactoe.model.GameState;

/**
 * Client-Skeleton für {@link GameListener}: nimmt die Notifications des Servers entgegen, wandelt das JSON in einen
 * {@link GameState} um (Unmarshalling) und ruft die echte Anzeige auf, den {@link tictactoe.ui.ConsoleListener}.
 */
public class GameListenerSkeleton implements Skeleton {
    private final GameListener listener;

    /**
     * Erzeugt das Skeleton.
     *
     * @param listener echte Implementierung, die den Zustand anzeigt
     */
    public GameListenerSkeleton(GameListener listener) { this.listener = listener; }

    @Override
    public JsonElement invoke(String method, JsonElement param) {
        if (!method.equals("onStateChanged")) throw new RpcException("Unbekannte Methode: " + method);
        listener.onStateChanged(RpcPeer.GSON.fromJson(param, GameState.class));
        return null;
    }
}
