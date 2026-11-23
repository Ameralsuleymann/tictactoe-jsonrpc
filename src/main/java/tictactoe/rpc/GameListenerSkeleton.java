package tictactoe.rpc;

import tictactoe.api.GameListener;
import tictactoe.model.GameState;

/**
 * Client-Skeleton für {@link GameListener}: nimmt die Nachrichten aus dem Topic {@value JmsPeer#STATE} und aus der
 * eigenen Antwort-Queue entgegen, wandelt den JSON-Body um (Unmarshalling) und ruft die echte Anzeige auf, den
 * {@link tictactoe.ui.ConsoleListener}.
 */
public class GameListenerSkeleton implements Skeleton {
    private final GameListener listener;

    /**
     * Erzeugt das Skeleton.
     *
     * @param listener echte Implementierung, die Zustand und Fehler anzeigt
     */
    public GameListenerSkeleton(GameListener listener) { this.listener = listener; }

    @Override
    public void invoke(Call call) {
        switch (call.method()) {
            case "onStateChanged" -> listener.onStateChanged(JmsPeer.GSON.fromJson(call.body(), GameState.class));
            case "onError" -> listener.onError(JmsPeer.GSON.fromJson(call.body(), String.class));
            default -> throw new RpcException("Unbekannte Methode: " + call.method());
        }
    }
}
