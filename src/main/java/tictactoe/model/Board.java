package tictactoe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO: 3x3-Spielfeld, unveränderlich (Record), Teil von {@link GameState}.
 *
 * <p>Zeilen und Spalten zählen von 0 bis 2. Ein Zug ändert das Spielfeld nicht, sondern
 * {@link #with(int, int, Symbol)} liefert ein neues. Dazu kommen einfache Hilfsmethoden für die Spiellogik
 * (Gewinn- und Vollprüfung) und die Konsolenausgabe.
 *
 * @param cells Felder als {@code cells.get(zeile).get(spalte)}; freie Felder sind {@link Symbol#EMPTY}
 */
public record Board(List<List<Symbol>> cells) {
    /** Kantenlänge des Spielfelds. */
    public static final int SIZE = 3;

    /**
     * Prüft die Größe (3x3) und speichert die Felder als unveränderliche Kopie.
     * Greift auch beim Unmarshalling, ein fehlerhaftes Spielfeld aus dem Netz wird also abgelehnt.
     *
     * @param cells Felder, zeilenweise
     * @throws IllegalArgumentException wenn das Spielfeld nicht 3x3 Felder hat
     */
    public Board {
        if (cells == null || cells.size() != SIZE || cells.stream().anyMatch(row -> row == null || row.size() != SIZE))
            throw new IllegalArgumentException("Spielfeld muss " + SIZE + "x" + SIZE + " Felder haben");
        cells = cells.stream().map(List::copyOf).toList();
    }

    /** Erzeugt ein leeres Spielfeld. */
    public Board() { this(Collections.nCopies(SIZE, Collections.nCopies(SIZE, Symbol.EMPTY))); }

    /**
     * Liefert den Inhalt eines Feldes.
     *
     * @param r Zeile (0 bis 2)
     * @param c Spalte (0 bis 2)
     * @return Symbol des Feldes, {@link Symbol#EMPTY} wenn es frei ist
     */
    public Symbol get(int r, int c) { return cells.get(r).get(c); }

    /**
     * Liefert ein neues Spielfeld mit einem gesetzten Feld; dieses Spielfeld bleibt unverändert.
     * Regeln prüft erst {@link tictactoe.game.Game}.
     *
     * @param r Zeile (0 bis 2)
     * @param c Spalte (0 bis 2)
     * @param s zu setzendes Symbol
     * @return neues Spielfeld
     */
    public Board with(int r, int c, Symbol s) {
        List<List<Symbol>> rows = new ArrayList<>();
        for (List<Symbol> row : cells) rows.add(new ArrayList<>(row));
        rows.get(r).set(c, s);
        return new Board(rows);
    }

    /**
     * Prüft, ob eine Position im Spielfeld liegt.
     *
     * @param r Zeile
     * @param c Spalte
     * @return {@code true}, wenn Zeile und Spalte jeweils zwischen 0 und 2 liegen
     */
    public boolean inBounds(int r, int c) { return r >= 0 && r < SIZE && c >= 0 && c < SIZE; }

    /**
     * Prüft, ob kein Feld mehr frei ist.
     *
     * @return {@code true}, wenn alle Felder belegt sind
     */
    public boolean isFull() {
        for (List<Symbol> row : cells) if (row.contains(Symbol.EMPTY)) return false;
        return true;
    }

    /**
     * Sucht drei gleiche Symbole in einer Zeile, Spalte oder Diagonale.
     *
     * @return {@link Symbol#X} oder {@link Symbol#O} bei drei in einer Reihe, sonst {@code null}
     */
    public Symbol winner() {
        for (int i = 0; i < SIZE; i++) {
            if (line(get(i, 0), get(i, 1), get(i, 2))) return get(i, 0);
            if (line(get(0, i), get(1, i), get(2, i))) return get(0, i);
        }
        if (line(get(0, 0), get(1, 1), get(2, 2))) return get(0, 0);
        if (line(get(0, 2), get(1, 1), get(2, 0))) return get(0, 2);
        return null;
    }

    /** Drei gleiche, nicht leere Symbole? */
    private static boolean line(Symbol a, Symbol b, Symbol c) { return a != Symbol.EMPTY && a == b && b == c; }

    /**
     * Stellt das Spielfeld mit Zeilen- und Spaltennummern für die Konsole dar.
     *
     * @return mehrzeilige Textdarstellung
     */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder("    0   1   2\n");
        for (int r = 0; r < SIZE; r++) {
            sb.append(r).append("   ");
            for (int c = 0; c < SIZE; c++) {
                sb.append(get(r, c) == Symbol.EMPTY ? " " : get(r, c).name());
                if (c < SIZE - 1) sb.append(" | ");
            }
            sb.append('\n');
            if (r < SIZE - 1) sb.append("    --+---+--\n");
        }
        return sb.toString();
    }
}
