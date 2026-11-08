package tictactoe.model;

import java.util.ArrayList;
import java.util.List;

/** DTO: kompletter Spielzustand (Server -> Client als Rueckgabewert und in Callbacks). */
public class GameState {
    private Board board = new Board();
    private List<Player> players = new ArrayList<>();
    private Symbol current = Symbol.X;
    private Status status = Status.WAITING;
    private Player winner;
    private int round = 1;

    public GameState() { }
    public GameState(Board board, List<Player> players, Symbol current, Status status, Player winner, int round) {
        this.board = board; this.players = players; this.current = current; this.status = status; this.winner = winner;
        this.round = round;
    }

    public Board getBoard() { return board; }
    public List<Player> getPlayers() { return players; }
    public Symbol getCurrent() { return current; }
    public Status getStatus() { return status; }
    public Player getWinner() { return winner; }
    /** Zaehlt hoch bei Neustart / neuer Runde. */
    public int getRound() { return round; }

    public Player currentPlayer() {
        for (Player p : players) if (p.getSymbol() == current) return p;
        return null;
    }
}
