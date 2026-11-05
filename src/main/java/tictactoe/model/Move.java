package tictactoe.model;

/** DTO: ein Zug (Client -> Server). Dient zugleich als einfaches Command-Objekt. */
public class Move {
    private Player player;
    private int row;
    private int col;

    public Move() { }
    public Move(Player player, int row, int col) { this.player = player; this.row = row; this.col = col; }

    public Player getPlayer() { return player; }
    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override public String toString() { return player + " -> (" + row + "," + col + ")"; }
}
