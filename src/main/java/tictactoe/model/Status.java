package tictactoe.model;

/**
 * Status einer Spielrunde; Teil von {@link GameState}.
 */
public enum Status {
    /** Es sind noch nicht zwei Spieler angemeldet; Zuege sind nicht moeglich. */
    WAITING,
    /** Die Runde laeuft; der Spieler mit {@link GameState#getCurrent()} ist am Zug. */
    RUNNING,
    /** Die Runde ist mit einem Gewinner beendet. */
    WON,
    /** Die Runde ist unentschieden beendet, das Spielfeld ist voll. */
    DRAW
}
