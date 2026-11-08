package tictactoe.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import tictactoe.api.*;
import tictactoe.model.*;

/** Spiellogik + Observer-Subject. Laeuft lokal im Prozess ODER auf dem Server -- identischer Code. */
public class Game implements GameService {
    private Board board = new Board();
    private final List<Player> players = new ArrayList<>();
    private Symbol current = Symbol.X;
    private Status status = Status.WAITING;
    private Player winner;
    private int round = 1;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(GameListener l) { listeners.add(l); }
    public void removeListener(GameListener l) { listeners.remove(l); }

    @Override
    public synchronized Player join(Player p) throws GameException {
        if (p == null || p.getName() == null || p.getName().isBlank()) throw new GameException("Name fehlt");
        if (players.size() >= 2) throw new GameException("Spiel ist bereits voll");
        if (find(p.getName()) != null) throw new GameException("Name '" + p.getName() + "' ist schon vergeben");
        Symbol free = find(Symbol.X) == null ? Symbol.X : Symbol.O;    // nach leave() kann auch X frei sein
        Player registered = new Player(p.getName(), free);
        players.add(registered);
        if (players.size() == 2) status = Status.RUNNING;
        for (GameListener l : listeners) notifySafe(() -> l.onPlayerJoined(registered), l);
        fireState();
        return new Player(registered.getName(), registered.getSymbol());
    }

    /** Spieler verlaesst das Spiel (Server: Verbindung getrennt). Laufende Runde endet, der Platz wird frei. */
    public synchronized void leave(String name) {
        Player p = find(name);
        if (p == null) return;
        players.remove(p);
        newRound();
        status = Status.WAITING;
        Player left = new Player(p.getName(), p.getSymbol());
        for (GameListener l : listeners) notifySafe(() -> l.onPlayerLeft(left), l);
        fireState();
    }

    @Override
    public synchronized GameState makeMove(Move m) throws GameException {
        if (status == Status.WAITING) throw new GameException("Warte auf zweiten Spieler");
        if (status != Status.RUNNING) throw new GameException("Spiel ist beendet - mit 'neu' weiterspielen");
        Player p = find(m.getPlayer().getName());
        if (p == null) throw new GameException("Unbekannter Spieler: " + m.getPlayer().getName());
        if (p.getSymbol() != current) throw new GameException("Du bist nicht am Zug");
        if (!board.inBounds(m.getRow(), m.getCol())) throw new GameException("Feld ausserhalb des Spielfelds");
        if (board.get(m.getRow(), m.getCol()) != Symbol.EMPTY) throw new GameException("Feld ist bereits belegt");

        board.set(m.getRow(), m.getCol(), p.getSymbol());
        Symbol w = board.winner();
        if (w != null) { status = Status.WON; winner = find(w); }
        else if (board.isFull()) status = Status.DRAW;
        else current = current.other();
        fireState();
        return getState();
    }

    @Override
    public synchronized GameState getState() {
        List<Player> copy = new ArrayList<>();
        for (Player p : players) copy.add(new Player(p.getName(), p.getSymbol()));
        Player w = winner == null ? null : new Player(winner.getName(), winner.getSymbol());
        return new GameState(board.copy(), copy, current, status, w, round);
    }

    /** Neustart der laufenden Runde bzw. neue Runde nach Spielende; die Spieler bleiben angemeldet. */
    @Override
    public synchronized void reset() {
        newRound();
        status = players.size() == 2 ? Status.RUNNING : Status.WAITING;
        fireState();
    }

    private void newRound() { board = new Board(); current = Symbol.X; winner = null; round++; }

    private void fireState() {
        GameState s = getState();
        for (GameListener l : listeners) notifySafe(() -> l.onStateChanged(s), l);
    }

    private void notifySafe(Runnable r, GameListener l) {
        try { r.run(); } catch (RuntimeException e) { listeners.remove(l); } // toter Client -> abmelden
    }

    private Player find(String name) { for (Player p : players) if (p.getName().equals(name)) return p; return null; }
    private Player find(Symbol s) { for (Player p : players) if (p.getSymbol() == s) return p; return null; }
}
