package tictactoe.model;

import java.util.Arrays;

/**
 * DTO: 3x3-Spielfeld, Bestandteil von {@link GameState}.
 *
 * <p>Zeilen und Spalten werden von 0 bis 2 gezaehlt. Gson serialisiert das Feld {@code cells} als
 * verschachteltes JSON-Array aus Symbolnamen. Neben den Daten enthaelt die Klasse einfache Hilfsmethoden fuer die
 * Spiellogik (Gewinn- und Vollpruefung) und die Konsolenausgabe.
 */
public class Board {
    /** Kantenlaenge des Spielfelds. */
    public static final int SIZE = 3;
    /** Inhalt der Felder als {@code cells[zeile][spalte]}; freie Felder sind {@link Symbol#EMPTY}. */
    private Symbol[][] cells = new Symbol[SIZE][SIZE];

    /**
     * Erzeugt ein leeres Spielfeld, in dem alle Felder {@link Symbol#EMPTY} sind.
     * Wird auch von Gson beim Unmarshalling verwendet.
     */
    public Board() {
        for (Symbol[] row : cells) Arrays.fill(row, Symbol.EMPTY);
    }

    /**
     * Liefert den Inhalt eines Feldes.
     *
     * @param r Zeile (0 bis 2)
     * @param c Spalte (0 bis 2)
     * @return Symbol des Feldes, {@link Symbol#EMPTY} wenn es frei ist
     */
    public Symbol get(int r, int c) { return cells[r][c]; }

    /**
     * Setzt ein Symbol in ein Feld, ohne Regeln zu pruefen; das uebernimmt {@link tictactoe.game.Game}.
     *
     * @param r Zeile (0 bis 2)
     * @param c Spalte (0 bis 2)
     * @param s zu setzendes Symbol
     */
    public void set(int r, int c, Symbol s) { cells[r][c] = s; }

    /**
     * Prueft, ob eine Position innerhalb des Spielfelds liegt.
     *
     * @param r Zeile
     * @param c Spalte
     * @return {@code true}, wenn Zeile und Spalte jeweils zwischen 0 und {@code SIZE - 1} liegen
     */
    public boolean inBounds(int r, int c) { return r >= 0 && r < SIZE && c >= 0 && c < SIZE; }

    /**
     * Prueft, ob kein Feld mehr frei ist.
     *
     * @return {@code true}, wenn alle Felder belegt sind (ohne Gewinner bedeutet das Unentschieden)
     */
    public boolean isFull() {
        for (Symbol[] row : cells) for (Symbol s : row) if (s == Symbol.EMPTY) return false;
        return true;
    }

    /**
     * Ermittelt den Gewinner, indem alle Zeilen, Spalten und beide Diagonalen auf drei gleiche Symbole geprueft werden.
     *
     * @return Symbol mit drei in einer Reihe ({@link Symbol#X} oder {@link Symbol#O}) oder {@code null}
     */
    public Symbol winner() {
        for (int i = 0; i < SIZE; i++) {
            if (line(cells[i][0], cells[i][1], cells[i][2])) return cells[i][0];
            if (line(cells[0][i], cells[1][i], cells[2][i])) return cells[0][i];
        }
        if (line(cells[0][0], cells[1][1], cells[2][2])) return cells[0][0];
        if (line(cells[0][2], cells[1][1], cells[2][0])) return cells[0][2];
        return null;
    }

    /**
     * Prueft, ob drei Felder dasselbe, nicht leere Symbol enthalten.
     *
     * @param a erstes Feld
     * @param b zweites Feld
     * @param c drittes Feld
     * @return {@code true} bei drei gleichen Symbolen ungleich {@link Symbol#EMPTY}
     */
    private static boolean line(Symbol a, Symbol b, Symbol c) { return a != Symbol.EMPTY && a == b && b == c; }

    /**
     * Erzeugt eine unabhaengige Kopie des Spielfelds.
     * Wird von {@link tictactoe.game.Game#getState()} genutzt, damit der interne Zustand nicht nach aussen gelangt.
     *
     * @return neues Spielfeld mit gleichem Inhalt
     */
    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) b.cells[r] = cells[r].clone();
        return b;
    }

    /**
     * Stellt das Spielfeld fuer die Konsole dar, mit Zeilen- und Spaltennummern.
     *
     * @return mehrzeilige Textdarstellung des Spielfelds
     */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder("    0   1   2\n");
        for (int r = 0; r < SIZE; r++) {
            sb.append(r).append("   ");
            for (int c = 0; c < SIZE; c++) {
                sb.append(cells[r][c] == Symbol.EMPTY ? " " : cells[r][c].name());
                if (c < SIZE - 1) sb.append(" | ");
            }
            sb.append('\n');
            if (r < SIZE - 1) sb.append("    --+---+--\n");
        }
        return sb.toString();
    }
}
