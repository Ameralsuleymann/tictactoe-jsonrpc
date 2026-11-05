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

    public GameSession(Game game, RpcPeer peer) {
        this.game = game;
        this.peer = peer;
        peer.onClose(() -> { if (listenerStub != null) game.removeListener(listenerStub); });
    }

    @Override public synchronized Player join(Player player) throws GameException {
        // Stub fuer den Rueckkanal Server -> Client; beim Client ist der Dienst "listener" registriert
        listenerStub = peer.proxy("listener", GameListener.class);
        game.addListener(listenerStub);
        try { return game.join(player); }
        catch (GameException e) { game.removeListener(listenerStub); throw e; }
    }

    @Override public GameState makeMove(Move move) throws GameException { return game.makeMove(move); }
    @Override public GameState getState() { return game.getState(); }
    @Override public void reset() { game.reset(); }
}
