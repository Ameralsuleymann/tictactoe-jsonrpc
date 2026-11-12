package tictactoe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO: vollstaendiger Spielzustand, vom Server zum Client als Rueckgabewert und in Callbacks.
 *
 * <p>Wird uebertragen als Rueckgabewert von {@link tictactoe.api.GameService#makeMove(Move)} und
 * {@link tictactoe.api.GameService#getState()} sowie im Callback
 * {@link tictactoe.api.GameListener#onStateChanged(GameState)}. Enthaelt selbst die DTOs {@link Board} und
 * {@link Player}. Der Client erhaelt immer den kompletten Zustand, keine Delta-Updates.
 */
public class GameState {
    /** Spielfeld. */
    private Board board = new Board();
    /** Angemeldete Spieler (0 bis 2). */
    private List<Player> players = new ArrayList<>();
    /** Symbol des Spielers, der am Zug ist. */
    private Symbol current = Symbol.X;
    /** Status der Runde. */
    private Status status = Status.WAITING;
    /** Gewinner der Runde oder {@code null}. */
    private Player winner;
    /** Rundennummer. */
    private int round = 1;

    /** Erzeugt einen leeren Anfangszustand; wird von Gson beim Unmarshalling verwendet. */
    public GameState() { }

    /**
     * Erzeugt einen vollstaendigen Spielzustand.
     *
     * @param board   Spielfeld
     * @param players angemeldete Spieler
     * @param current Symbol des Spielers, der am Zug ist
     * @param status  Status der Runde
     * @param winner  Gewinner oder {@code null}
     * @param round   Rundennummer
     */
    public GameState(Board board, List<Player> players, Symbol current, Status status, Player winner, int round) {
        this.board = board; this.players = players; this.current = current; this.status = status; this.winner = winner;
        this.round = round;
    }

    /**
     * Liefert das Spielfeld.
     *
     * @return Spielfeld dieses Zustands
     */
    public Board getBoard() { return board; }

    /**
     * Liefert die angemeldeten Spieler.
     *
     * @return Liste der Spieler (0 bis 2 Eintraege)
     */
    public List<Player> getPlayers() { return players; }

    /**
     * Liefert das Symbol des Spielers, der am Zug ist.
     *
     * @return {@link Symbol#X} oder {@link Symbol#O}
     */
    public Symbol getCurrent() { return current; }

    /**
     * Liefert den Status der Runde.
     *
     * @return aktueller Status
     */
    public Status getStatus() { return status; }

    /**
     * Liefert den Gewinner der Runde.
     *
     * @return Gewinner oder {@code null}, solange es keinen gibt
     */
    public Player getWinner() { return winner; }

    /**
     * Liefert die Rundennummer; sie zaehlt hoch bei Neustart oder neuer Runde.
     * Der Client erkennt daran den Beginn einer neuen Runde.
     *
     * @return Rundennummer, beginnend bei 1
     */
    public int getRound() { return round; }

    /**
     * Ermittelt den Spieler, der am Zug ist.
     *
     * @return Spieler mit dem Symbol {@link #getCurrent()} oder {@code null}, wenn dieser nicht angemeldet ist
     */
    public Player currentPlayer() {
        for (Player p : players) if (p.getSymbol() == current) return p;
        return null;
    }
}
