package tictactoe.model;

/**
 * DTO: ein Zug eines Spielers, vom Client zum Server als Parameter von
 * {@link tictactoe.api.GameService#makeMove(Move)}.
 *
 * <p>Dient zugleich als einfaches Command-Objekt: Der Zug wird als Objekt gekapselt und erst von
 * {@link tictactoe.game.Game} geprueft und ausgefuehrt. Enthaelt selbst das DTO {@link Player}.
 */
public class Move {
    /** Spieler, der den Zug ausfuehrt; massgeblich ist sein Name. */
    private Player player;
    /** Zeile (0 bis 2). */
    private int row;
    /** Spalte (0 bis 2). */
    private int col;

    /** Erzeugt einen leeren Zug; wird von Gson beim Unmarshalling verwendet. */
    public Move() { }

    /**
     * Erzeugt einen Zug.
     *
     * @param player Spieler, der den Zug ausfuehrt
     * @param row    Zeile (0 bis 2)
     * @param col    Spalte (0 bis 2)
     */
    public Move(Player player, int row, int col) { this.player = player; this.row = row; this.col = col; }

    /**
     * Liefert den Spieler, der den Zug ausfuehrt.
     *
     * @return ziehender Spieler
     */
    public Player getPlayer() { return player; }

    /**
     * Liefert die Zeile des Zuges.
     *
     * @return Zeile (0 bis 2)
     */
    public int getRow() { return row; }

    /**
     * Liefert die Spalte des Zuges.
     *
     * @return Spalte (0 bis 2)
     */
    public int getCol() { return col; }

    /**
     * Textdarstellung fuer Konsole und Debugging.
     *
     * @return Spieler und Position, z.B. {@code Alice (X) -> (1,1)}
     */
    @Override public String toString() { return player + " -> (" + row + "," + col + ")"; }
}
