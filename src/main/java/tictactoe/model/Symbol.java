package tictactoe.model;

/**
 * Spielsteine und Feldinhalte; EMPTY steht fuer ein freies Feld.
 */
public enum Symbol {
    /** Spielstein des ersten Spielers; X beginnt jede Runde. */
    X,
    /** Spielstein des zweiten Spielers. */
    O,
    /** Freies Feld ohne Spielstein. */
    EMPTY;

    /**
     * Liefert den Spielstein des Gegenspielers.
     *
     * @return {@link #O} fuer {@link #X}, sonst {@link #X}
     */
    public Symbol other() { return this == X ? O : X; }
}
