package tictactoe.game;

import java.util.ArrayList;
import java.util.List;
import tictactoe.api.*;
import tictactoe.model.*;

/**
 * Spiellogik von Tic-Tac-Toe und Subject des Observer-Patterns.
 *
 * <p>Läuft unverändert lokal ({@link tictactoe.local.LocalMain}) und auf dem Server, wo sich alle
 * {@link tictactoe.server.GameSession}s eine Instanz teilen. Regeln, Zugreihenfolge und Gewinnprüfung liegen nur hier
 * (Thin Client). Alle öffentlichen Methoden sind {@code synchronized}, weil auf dem Server mehrere Clients gleichzeitig
 * zugreifen. Nach jeder Änderung erhalten alle Listener den neuen Zustand.
 */
public class Game implements GameService {
    /** Aktuelles Spielfeld; wird bei jedem Zug durch ein neues ersetzt. */
    private Board board = new Board();
    /** Angemeldete Spieler (höchstens zwei). */
    private final List<Player> players = new ArrayList<>();
    /** Symbol des Spielers am Zug; nach einem Sieg das Symbol des Gewinners. */
    private Symbol current = Symbol.X;
    /** Status der aktuellen Runde. */
    private Status status = Status.WAITING;
    /** Beobachter: lokal der ConsoleListener, auf dem Server ein GameListenerStub pro Client. */
    private final List<GameListener> listeners = new ArrayList<>();

    /**
     * Meldet einen Beobachter an.
     *
     * @param l Beobachter, der ab jetzt jeden neuen Zustand erhält
     */
    public synchronized void addListener(GameListener l) { listeners.add(l); }

    /**
     * Meldet einen Beobachter ab.
     *
     * @param l abzumeldender Beobachter
     */
    public synchronized void removeListener(GameListener l) { listeners.remove(l); }

    @Override
    public synchronized void join(Player p) throws GameException {
        if (p == null || p.name() == null || p.name().isBlank()) throw new GameException("Name fehlt");
        if (players.size() == 2) throw new GameException("Spiel ist bereits voll");
        if (find(p.name()) != null) throw new GameException("Name '" + p.name() + "' ist schon vergeben");
        // Nach leave() kann X frei sein, während O noch im Spiel ist
        Symbol free = (players.isEmpty() || players.get(0).symbol() == Symbol.O) ? Symbol.X : Symbol.O;
        players.add(new Player(p.name(), free));
        newRound();
        fireState();
    }

    /**
     * Spieler verlässt das Spiel (auf dem Server: Verbindung getrennt). Die Runde beginnt neu und wartet auf einen
     * zweiten Spieler; unbekannte Namen werden ignoriert.
     *
     * @param name Name des Spielers
     */
    public synchronized void leave(String name) {
        Player p = find(name);
        if (p == null) return;
        players.remove(p);
        newRound();
        fireState();
    }

    @Override
    public synchronized void makeMove(Move m) throws GameException {
        if (status == Status.WAITING) throw new GameException("Warte auf zweiten Spieler");
        if (status != Status.RUNNING) throw new GameException("Runde ist beendet - mit 'neu' weiterspielen");
        Player p = find(m.player().name());
        if (p == null) throw new GameException("Unbekannter Spieler: " + m.player().name());
        if (p.symbol() != current) throw new GameException("Du bist nicht am Zug");
        if (!board.inBounds(m.row(), m.col())) throw new GameException("Feld außerhalb des Spielfelds");
        if (board.get(m.row(), m.col()) != Symbol.EMPTY) throw new GameException("Feld ist bereits belegt");

        board = board.with(m.row(), m.col(), current);
        if (board.winner() != null) status = Status.WON;      // current bleibt stehen: er ist der Gewinner
        else if (board.isFull()) status = Status.DRAW;
        else current = current.other();
        fireState();
    }

    @Override
    public synchronized void reset() throws GameException {
        if (players.size() < 2) throw new GameException("Warte auf zweiten Spieler");
        newRound();
        fireState();
    }

    /** Leeres Spielfeld, X beginnt; die Runde läuft, sobald zwei Spieler angemeldet sind. */
    private void newRound() {
        board = new Board();
        current = Symbol.X;
        status = players.size() == 2 ? Status.RUNNING : Status.WAITING;
    }

    /** Observer: schickt allen Listenern den aktuellen Zustand. */
    private void fireState() {
        GameState state = new GameState(board, players, current, status);
        for (GameListener l : listeners) l.onStateChanged(state);
    }

    /** Sucht einen angemeldeten Spieler über seinen Namen; {@code null}, wenn es keinen gibt. */
    private Player find(String name) {
        for (Player p : players) if (p.name().equals(name)) return p;
        return null;
    }
}
