package tictactoe.model;

/** Spielsteine und Feldinhalte; {@link #EMPTY} steht für ein freies Feld. */
public enum Symbol {
    /** Spielstein des ersten Spielers; X beginnt jede Runde. */
    X,
    /** Spielstein des zweiten Spielers. */
    O,
    /** Freies Feld. */
    EMPTY;

    /**
     * Liefert den Spielstein des Gegenspielers.
     *
     * @return {@link #O} für {@link #X}, sonst {@link #X}
     */
    public Symbol other() { return this == X ? O : X; }
}
