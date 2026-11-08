package tictactoe.server;

import tictactoe.api.*;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.rpc.RpcPeer;

/** Server-seitiger Endpunkt pro verbundenem Client; delegiert an das gemeinsame Game. */
public class GameSession implements GameService {
    private final Game game;
    private final RpcPeer peer;
    private GameListener listenerStub;
    private Player player;   // gesetzt nach erfolgreichem join

    public GameSession(Game game, RpcPeer peer) {
        this.game = game;
        this.peer = peer;
        peer.onClose(this::disconnected);
    }

    @Override public synchronized Player join(Player p) throws GameException {
        if (player != null) throw new GameException("Du bist bereits als " + player + " im Spiel");
        // Stub fuer den Rueckkanal Server -> Client; beim Client ist der Dienst "listener" registriert
        listenerStub = peer.proxy("listener", GameListener.class);
        game.addListener(listenerStub);
        try { player = game.join(p); return player; }
        catch (GameException e) { game.removeListener(listenerStub); listenerStub = null; throw e; }
    }

    @Override public GameState makeMove(Move move) throws GameException { return game.makeMove(move); }
    @Override public GameState getState() { return game.getState(); }
    @Override public void reset() { game.reset(); }

    /** Client weg (q oder Abbruch): Callback abmelden und Platz freigeben, damit jemand neu beitreten kann. */
    private synchronized void disconnected() {
        if (listenerStub != null) game.removeListener(listenerStub);
        if (player == null) return;
        System.out.println("Verbindung getrennt: " + player + " - Platz ist wieder frei.");
        game.leave(player.getName());
        player = null;
    }
}
