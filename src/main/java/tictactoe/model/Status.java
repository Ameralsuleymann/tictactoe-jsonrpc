package tictactoe.model;

/** Status einer Spielrunde; Teil von {@link GameState}. */
public enum Status {
    /** Noch keine zwei Spieler angemeldet; Züge sind nicht möglich. */
    WAITING,
    /** Die Runde läuft; der Spieler mit {@link GameState#current()} ist am Zug. */
    RUNNING,
    /** Die Runde ist entschieden; {@link GameState#current()} ist der Gewinner. */
    WON,
    /** Unentschieden, das Spielfeld ist voll. */
    DRAW
}
