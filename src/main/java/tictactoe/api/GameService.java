package tictactoe.api;

import tictactoe.model.GameState;
import tictactoe.model.Move;
import tictactoe.model.Player;

/** C-S-Contract, Richtung Client -> Server. Auch lokal von Game implementiert (Zugriffstransparenz). */
public interface GameService {
    /** Meldet einen Spieler an; liefert ihn mit zugewiesenem Symbol zurueck. */
    Player join(Player player) throws GameException;
    GameState makeMove(Move move) throws GameException;
    GameState getState();
    void reset();
}
