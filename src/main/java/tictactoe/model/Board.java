package tictactoe.model;

import java.util.Arrays;

/** DTO: 3x3-Spielfeld (Teil von GameState). */
public class Board {
    public static final int SIZE = 3;
    private Symbol[][] cells = new Symbol[SIZE][SIZE];

    public Board() {
        for (Symbol[] row : cells) Arrays.fill(row, Symbol.EMPTY);
    }

    public Symbol get(int r, int c) { return cells[r][c]; }
    public void set(int r, int c, Symbol s) { cells[r][c] = s; }
    public boolean inBounds(int r, int c) { return r >= 0 && r < SIZE && c >= 0 && c < SIZE; }

    public boolean isFull() {
        for (Symbol[] row : cells) for (Symbol s : row) if (s == Symbol.EMPTY) return false;
        return true;
    }

    /** @return Symbol mit drei in einer Reihe oder null */
    public Symbol winner() {
        for (int i = 0; i < SIZE; i++) {
            if (line(cells[i][0], cells[i][1], cells[i][2])) return cells[i][0];
            if (line(cells[0][i], cells[1][i], cells[2][i])) return cells[0][i];
        }
        if (line(cells[0][0], cells[1][1], cells[2][2])) return cells[0][0];
        if (line(cells[0][2], cells[1][1], cells[2][0])) return cells[0][2];
        return null;
    }

    private static boolean line(Symbol a, Symbol b, Symbol c) { return a != Symbol.EMPTY && a == b && b == c; }

    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) b.cells[r] = cells[r].clone();
        return b;
    }

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
