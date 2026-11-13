package tictactoe.model;

/**
 * DTO: ein Zug eines Spielers, unveraenderlich (Record), vom Client zum Server als Parameter von
 * {@link tictactoe.api.GameService#makeMove(Move)}.
 *
 * <p>Dient zugleich als einfaches Command-Objekt: Der Zug wird als Objekt gekapselt und erst von
 * {@link tictactoe.game.Game} geprueft und ausgefuehrt. Enthaelt selbst das DTO {@link Player}.
 *
 * @param player Spieler, der den Zug ausfuehrt; massgeblich ist sein Name
 * @param row    Zeile (0 bis 2)
 * @param col    Spalte (0 bis 2)
 */
public record Move(Player player, int row, int col) {
    /**
     * Textdarstellung fuer Konsole und Debugging.
     *
     * @return Spieler und Position, z.B. {@code Alice (X) -> (1,1)}
     */
    @Override public String toString() { return player + " -> (" + row + "," + col + ")"; }
}
