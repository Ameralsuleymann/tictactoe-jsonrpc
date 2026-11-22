package tictactoe.server;

import tictactoe.api.*;
import tictactoe.game.Game;
import tictactoe.model.*;

/**
 * Sitzung eines verbundenen Clients auf dem Server; eine Anwendungsklasse, keine Kommunikation.
 *
 * <p>Leitet alle Aufrufe an das gemeinsame {@link Game} weiter und merkt sich, welcher Spieler und welcher
 * Listener-Stub zu diesem Client gehören, damit beide beim Verbindungsende abgemeldet werden können.
 * Alle Aufrufe eines Clients laufen nacheinander im selben Reader-Thread, deshalb braucht die Session keine
 * Synchronisation.
 */
public class GameSession implements GameService {
    private final Game game;
    /** Stub, über den dieser Client den Spielzustand erhält. */
    private final GameListener listener;
    /** Spieler dieses Clients; {@code null}, bis der Beitritt geklappt hat. */
    private Player player;

    /**
     * Erzeugt die Session und meldet den Listener-Stub am Spiel an.
     *
     * @param game     gemeinsames Spiel aller Clients
     * @param listener Stub zu diesem Client
     */
    public GameSession(Game game, GameListener listener) {
        this.game = game;
        this.listener = listener;
        game.addListener(listener);
    }

    @Override
    public void join(Player p) throws GameException {
        if (player != null) throw new GameException("Du bist bereits als " + player + " im Spiel");
        game.join(p);
        player = p;
    }

    @Override public void makeMove(Move move) throws GameException { game.makeMove(move); }

    @Override public void reset() throws GameException { game.reset(); }

    /** Client weg ({@code q} oder Abbruch): Listener abmelden und den Platz im Spiel freigeben. */
    public void leave() {
        game.removeListener(listener);
        if (player == null) return;
        System.out.println("Verbindung getrennt: " + player + " - Platz ist wieder frei.");
        game.leave(player.name());
    }
}
