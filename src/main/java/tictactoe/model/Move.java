package tictactoe.model;

/**
 * DTO: ein Zug, unveränderlich (Record), vom Client zum Server bei
 * {@link tictactoe.api.GameService#makeMove(Move)}.
 *
 * <p>Zugleich ein einfaches Command-Objekt: Der Zug wird als Objekt verschickt und erst von
 * {@link tictactoe.game.Game} geprüft und ausgeführt.
 *
 * @param player Spieler, der zieht; maßgeblich ist sein Name
 * @param row    Zeile (0 bis 2)
 * @param col    Spalte (0 bis 2)
 */
public record Move(Player player, int row, int col) { }
