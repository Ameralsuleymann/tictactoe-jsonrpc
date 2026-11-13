package tictactoe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO: 3x3-Spielfeld, unveraenderlich (Record), Bestandteil von {@link GameState}.
 *
 * <p>Zeilen und Spalten werden von 0 bis 2 gezaehlt. Ein Zug veraendert das Spielfeld nicht, sondern
 * {@link #with(int, int, Symbol)} liefert ein neues Spielfeld; {@link tictactoe.game.Game} ersetzt damit sein
 * aktuelles Spielfeld. Gson serialisiert {@code cells} wie bisher als verschachteltes JSON-Array aus Symbolnamen.
 * Neben den Daten enthaelt die Klasse einfache Hilfsmethoden fuer die Spiellogik (Gewinn- und Vollpruefung) und die
 * Konsolenausgabe.
 *
 * @param cells Inhalt der Felder als {@code cells.get(zeile).get(spalte)}; freie Felder sind {@link Symbol#EMPTY}
 */
public record Board(List<List<Symbol>> cells) {
    /** Kantenlaenge des Spielfelds. */
    public static final int SIZE = 3;

    /**
     * Prueft die Groesse (3x3) und speichert die Felder als unveraenderliche Kopie.
     * Greift auch beim Unmarshalling, ein fehlerhaftes Spielfeld aus dem Netz wird also abgelehnt.
     *
     * @param cells Inhalt der Felder, zeilenweise
     * @throws IllegalArgumentException wenn das Spielfeld nicht 3x3 Felder hat
     * @throws NullPointerException     wenn ein Feld {@code null} ist
     */
    public Board {
        if (cells == null || cells.size() != SIZE || cells.stream().anyMatch(row -> row == null || row.size() != SIZE))
            throw new IllegalArgumentException("Spielfeld muss " + SIZE + "x" + SIZE + " Felder haben");
        cells = cells.stream().map(List::copyOf).toList();
    }

    /** Erzeugt ein leeres Spielfeld, in dem alle Felder {@link Symbol#EMPTY} sind. */
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
     * Liefert ein neues Spielfeld, in dem ein Feld geaendert ist; dieses Spielfeld bleibt unveraendert.
     * Regeln werden nicht geprueft, das uebernimmt {@link tictactoe.game.Game}.
     *
     * @param r Zeile (0 bis 2)
     * @param c Spalte (0 bis 2)
     * @param s zu setzendes Symbol
     * @return neues Spielfeld mit dem gesetzten Symbol
     */
    public Board with(int r, int c, Symbol s) {
        List<List<Symbol>> rows = new ArrayList<>();
        for (List<Symbol> row : cells) rows.add(new ArrayList<>(row));
        rows.get(r).set(c, s);
        return new Board(rows);
    }

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
        for (List<Symbol> row : cells) if (row.contains(Symbol.EMPTY)) return false;
        return true;
    }

    /**
     * Ermittelt den Gewinner, indem alle Zeilen, Spalten und beide Diagonalen auf drei gleiche Symbole geprueft werden.
     *
     * @return Symbol mit drei in einer Reihe ({@link Symbol#X} oder {@link Symbol#O}) oder {@code null}
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
     * Stellt das Spielfeld fuer die Konsole dar, mit Zeilen- und Spaltennummern.
     *
     * @return mehrzeilige Textdarstellung des Spielfelds
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
