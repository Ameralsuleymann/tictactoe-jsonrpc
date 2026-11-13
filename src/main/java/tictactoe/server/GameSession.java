package tictactoe.server;

import tictactoe.api.*;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.rpc.RpcPeer;

/**
 * Serverseitiger Endpunkt pro verbundenem Client; delegiert an das gemeinsame {@link Game}.
 *
 * <p>Wird beim {@link RpcPeer} der Client-Verbindung als Dienst {@code "game"} registriert. Beim Beitritt erzeugt
 * die Session den Stub fuer den Rueckkanal Server &rarr; Client ({@link GameListener}, beim Client als Dienst
 * {@code "listener"} registriert) und meldet ihn als Beobachter am Spiel an. Endet die Verbindung, werden
 * Beobachter und Spieler wieder abgemeldet.
 */
public class GameSession implements GameService {
    /** Das von allen Sessions gemeinsam genutzte Spiel. */
    private final Game game;
    /** RPC-Endpunkt der Verbindung zu diesem Client. */
    private final RpcPeer peer;
    /** Stub fuer Callbacks an diesen Client; gesetzt beim Beitritt, sonst {@code null}. */
    private GameListener listenerStub;
    /** Der ueber diese Session angemeldete Spieler; {@code null}, solange kein Beitritt erfolgt ist. */
    private Player player;   // gesetzt nach erfolgreichem join

    /**
     * Erzeugt eine Session und registriert die Abmeldung fuer das Verbindungsende.
     *
     * @param game gemeinsames Spiel aller Clients
     * @param peer RPC-Endpunkt der Verbindung zu diesem Client
     */
    public GameSession(Game game, RpcPeer peer) {
        this.game = game;
        this.peer = peer;
        peer.onClose(this::disconnected);
    }

    /**
     * Meldet den Client am Spiel an.
     *
     * <p>Vorher wird der Callback-Stub erzeugt und als Beobachter registriert, damit der Client bereits die
     * Ereignisse seines eigenen Beitritts erhaelt. Scheitert der Beitritt, wird der Stub wieder abgemeldet.
     *
     * @param p anzumeldender Spieler
     * @return der angemeldete Spieler mit zugewiesenem Symbol
     * @throws GameException wenn dieser Client bereits angemeldet ist oder das Spiel den Beitritt ablehnt
     */
    @Override public synchronized Player join(Player p) throws GameException {
        if (player != null) throw new GameException("Du bist bereits als " + player + " im Spiel");
        // Stub fuer den Rueckkanal Server -> Client; beim Client ist der Dienst "listener" registriert
        listenerStub = peer.proxy("listener", GameListener.class);
        game.addListener(listenerStub);
        try { player = game.join(p); return player; }
        catch (GameException e) { game.removeListener(listenerStub); listenerStub = null; throw e; }
    }

    /**
     * Delegiert den Zug an das gemeinsame Spiel.
     *
     * @param move der Zug
     * @return Spielzustand nach dem Zug
     * @throws GameException bei einem Regelverstoss
     */
    @Override public GameState makeMove(Move move) throws GameException { return game.makeMove(move); }

    /**
     * Liefert den Zustand des gemeinsamen Spiels.
     *
     * @return aktueller Spielzustand
     */
    @Override public GameState getState() { return game.getState(); }

    /** Delegiert den Neustart an das gemeinsame Spiel. */
    @Override public void reset() { game.reset(); }

    /**
     * Client weg ({@code q} oder Abbruch): Callback abmelden und Platz freigeben, damit jemand neu beitreten kann.
     * Wird vom {@link RpcPeer} im Reader-Thread aufgerufen, sobald die Verbindung endet.
     */
    private synchronized void disconnected() {
        if (listenerStub != null) game.removeListener(listenerStub);
        if (player == null) return;
        System.out.println("Verbindung getrennt: " + player + " - Platz ist wieder frei.");
        game.leave(player.name());
        player = null;
    }
}
