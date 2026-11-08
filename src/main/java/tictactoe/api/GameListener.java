package tictactoe.api;

import tictactoe.model.GameState;
import tictactoe.model.Player;

/** C-S-Contract, Richtung Server -> Client (Callback / Observer). Void = Notification (fire-and-forget). */
public interface GameListener {
    void onPlayerJoined(Player player);
    void onPlayerLeft(Player player);
    void onStateChanged(GameState state);
}
