package tictactoe.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import tictactoe.api.*;
import tictactoe.model.*;

/**
 * Spiellogik von Tic-Tac-Toe und Subject des Observer-Patterns.
 *
 * <p>Dieselbe Klasse laeuft unveraendert lokal im Prozess ({@link tictactoe.local.LocalMain}) oder auf dem
 * Server ({@link tictactoe.server.GameServer}), wo sich alle {@link tictactoe.server.GameSession}s eine Instanz
 * teilen. Regeln, Zugreihenfolge und Gewinnpruefung liegen ausschliesslich hier (Thin Client).
 *
 * <p>Alle oeffentlichen Spielmethoden sind {@code synchronized}, da auf dem Server mehrere Sessions gleichzeitig
 * zugreifen. Nach jeder Aenderung werden die registrierten {@link GameListener} benachrichtigt.
 *
 * <p>Da alle DTOs unveraenderlich sind ({@link Board}, {@link Player}, {@link GameState}), kann {@code Game} sie ohne
 * Kopien herausgeben: Niemand kann den internen Zustand ueber ein herausgegebenes Objekt veraendern. Ein Zug ersetzt
 * das Spielfeld durch ein neues ({@link Board#with(int, int, Symbol)}).
 */
public class Game implements GameService {
    /** Aktuelles Spielfeld (unveraenderlich); wird bei jedem Zug und jeder neuen Runde durch ein neues ersetzt. */
    private Board board = new Board();
    /** Angemeldete Spieler (hoechstens zwei). */
    private final List<Player> players = new ArrayList<>();
    /** Symbol des Spielers, der am Zug ist; X beginnt jede Runde. */
    private Symbol current = Symbol.X;
    /** Status der aktuellen Runde. */
    private Status status = Status.WAITING;
    /** Gewinner der aktuellen Runde oder {@code null}. */
    private Player winner;
    /** Rundenzaehler; wird bei jedem Neustart und jeder neuen Runde erhoeht. */
    private int round = 1;
    /**
     * Registrierte Beobachter (lokal der {@link tictactoe.ui.ConsoleListener}, auf dem Server je Client ein Stub).
     * Als {@link CopyOnWriteArrayList}, damit waehrend einer Benachrichtigung Beobachter entfernt werden koennen.
     */
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    /** Erzeugt ein neues Spiel ohne Spieler im Status {@link Status#WAITING} (Runde 1, X beginnt). */
    public Game() { }

    /**
     * Registriert einen Beobachter, der ab sofort ueber alle Spielereignisse informiert wird.
     *
     * @param l der Beobachter (lokal z.B. ein {@link tictactoe.ui.ConsoleListener}, auf dem Server ein Stub)
     */
    public void addListener(GameListener l) { listeners.add(l); }

    /**
     * Meldet einen Beobachter ab.
     *
     * @param l der abzumeldende Beobachter
     */
    public void removeListener(GameListener l) { listeners.remove(l); }

    /**
     * {@inheritDoc}
     *
     * <p>Vergeben wird das freie Symbol; nach einem {@link #leave(String)} kann auch wieder X frei sein. Mit dem
     * zweiten Spieler wechselt der Status auf {@link Status#RUNNING}. Alle Beobachter erhalten
     * {@link GameListener#onPlayerJoined(Player)} und {@link GameListener#onStateChanged(GameState)}.
     *
     * @param p anzumeldender Spieler; ausgewertet wird nur der Name
     * @return neuer, unveraenderlicher Spieler mit zugewiesenem Symbol
     * @throws GameException wenn der Name fehlt oder schon vergeben ist oder das Spiel bereits voll ist
     */
    @Override
    public synchronized Player join(Player p) throws GameException {
        if (p == null || p.name() == null || p.name().isBlank()) throw new GameException("Name fehlt");
        if (players.size() >= 2) throw new GameException("Spiel ist bereits voll");
        if (find(p.name()) != null) throw new GameException("Name '" + p.name() + "' ist schon vergeben");
        Symbol free = find(Symbol.X) == null ? Symbol.X : Symbol.O;    // nach leave() kann auch X frei sein
        Player registered = new Player(p.name(), free);                  // Symbol vergeben = neuer Player
        players.add(registered);
        if (players.size() == 2) status = Status.RUNNING;
        for (GameListener l : listeners) notifySafe(() -> l.onPlayerJoined(registered), l);
        fireState();
        return registered;
    }

    /**
     * Spieler verlaesst das Spiel (Server: Verbindung getrennt); die laufende Runde endet und der Platz wird frei.
     *
     * <p>Es beginnt eine neue, leere Runde im Status {@link Status#WAITING}. Alle Beobachter erhalten
     * {@link GameListener#onPlayerLeft(Player)} und den neuen Zustand. Unbekannte Namen werden ignoriert.
     *
     * @param name Name des Spielers, der das Spiel verlaesst
     */
    public synchronized void leave(String name) {
        Player p = find(name);
        if (p == null) return;
        players.remove(p);
        newRound();
        status = Status.WAITING;
        for (GameListener l : listeners) notifySafe(() -> l.onPlayerLeft(p), l);
        fireState();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Geprueft wird der Reihe nach: Spiel laeuft, Spieler ist bekannt und am Zug, Feld liegt im Spielfeld und ist
     * frei. Danach ersetzt ein neues Spielfeld mit dem gesetzten Symbol das alte, und es wird auf Gewinn oder
     * Unentschieden geprueft; andernfalls wechselt das Zugrecht zum anderen Spieler. Alle Beobachter erhalten den
     * neuen Zustand.
     *
     * @param m der Zug
     * @return Spielzustand nach dem Zug
     * @throws GameException bei einem Regelverstoss; die Meldung beschreibt den Grund
     */
    @Override
    public synchronized GameState makeMove(Move m) throws GameException {
        if (status == Status.WAITING) throw new GameException("Warte auf zweiten Spieler");
        if (status != Status.RUNNING) throw new GameException("Spiel ist beendet - mit 'neu' weiterspielen");
        Player p = find(m.player().name());
        if (p == null) throw new GameException("Unbekannter Spieler: " + m.player().name());
        if (p.symbol() != current) throw new GameException("Du bist nicht am Zug");
        if (!board.inBounds(m.row(), m.col())) throw new GameException("Feld ausserhalb des Spielfelds");
        if (board.get(m.row(), m.col()) != Symbol.EMPTY) throw new GameException("Feld ist bereits belegt");

        board = board.with(m.row(), m.col(), p.symbol());   // neues, unveraenderliches Spielfeld
        Symbol w = board.winner();
        if (w != null) { status = Status.WON; winner = find(w); }
        else if (board.isFull()) status = Status.DRAW;
        else current = current.other();
        fireState();
        return getState();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Spielfeld und Spieler sind unveraenderlich und werden deshalb nicht kopiert; nur die Spielerliste uebernimmt
     * {@link GameState} als unveraenderliche Kopie. Aufrufer koennen den internen Zustand so nicht veraendern.
     *
     * @return unveraenderlicher Schnappschuss des aktuellen Zustands
     */
    @Override
    public synchronized GameState getState() {
        return new GameState(board, players, current, status, winner, round);
    }

    /**
     * Neustart der laufenden Runde oder neue Runde nach Spielende; die Spieler bleiben angemeldet.
     *
     * <p>Der Rundenzaehler wird erhoeht. Sind zwei Spieler angemeldet, laeuft die neue Runde sofort, sonst wird auf
     * den zweiten Spieler gewartet. Alle Beobachter erhalten den neuen Zustand.
     */
    @Override
    public synchronized void reset() {
        newRound();
        status = players.size() == 2 ? Status.RUNNING : Status.WAITING;
        fireState();
    }

    /**
     * Setzt Spielfeld, Zugrecht (X beginnt) und Gewinner zurueck und erhoeht den Rundenzaehler.
     * Den Status setzt der Aufrufer.
     */
    private void newRound() { board = new Board(); current = Symbol.X; winner = null; round++; }

    /** Benachrichtigt alle Beobachter mit dem aktuellen Zustand ({@link GameListener#onStateChanged(GameState)}). */
    private void fireState() {
        GameState s = getState();
        for (GameListener l : listeners) notifySafe(() -> l.onStateChanged(s), l);
    }

    /**
     * Fuehrt eine Benachrichtigung aus und meldet den Beobachter ab, falls sie fehlschlaegt.
     *
     * <p>Auf dem Server bedeutet eine {@link RuntimeException} (z.B. {@link tictactoe.rpc.RpcException}) meist, dass
     * der Client nicht mehr erreichbar ist. So kann ein toter Client das Spiel nicht blockieren.
     *
     * @param r die eigentliche Benachrichtigung
     * @param l der zu benachrichtigende Beobachter
     */
    private void notifySafe(Runnable r, GameListener l) {
        try { r.run(); } catch (RuntimeException e) { listeners.remove(l); } // toter Client -> abmelden
    }

    /**
     * Sucht einen angemeldeten Spieler anhand seines Namens.
     *
     * @param name gesuchter Name
     * @return der Spieler oder {@code null}, wenn kein Spieler so heisst
     */
    private Player find(String name) { for (Player p : players) if (p.name().equals(name)) return p; return null; }

    /**
     * Sucht einen angemeldeten Spieler anhand seines Symbols.
     *
     * @param s gesuchtes Symbol
     * @return der Spieler oder {@code null}, wenn kein Spieler dieses Symbol hat
     */
    private Player find(Symbol s) { for (Player p : players) if (p.symbol() == s) return p; return null; }
}
