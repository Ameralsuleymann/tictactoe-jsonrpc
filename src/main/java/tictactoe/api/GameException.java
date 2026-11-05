package tictactoe.api;

/** Fachliche Exception; wird ueber RPC als JSON-Fehler uebertragen und beim Client neu geworfen. */
public class GameException extends Exception {
    public GameException(String message) { super(message); }
}
