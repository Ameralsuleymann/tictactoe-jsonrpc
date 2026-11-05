package tictactoe.model;

/** Spielsteine. EMPTY = freies Feld. */
public enum Symbol {
    X, O, EMPTY;

    public Symbol other() { return this == X ? O : X; }
}
