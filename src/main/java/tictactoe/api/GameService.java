package tictactoe.api;

import tictactoe.model.Move;
import tictactoe.model.Player;

/**
 * Contract Client &rarr; Server: alles, was ein Spieler am Spiel tun kann.
 *
 * <p>Implementierungen:
 * <ul>
 *   <li>{@link tictactoe.game.Game}: die Spiellogik (lokal oder auf dem Server),</li>
 *   <li>{@link tictactoe.rpc.GameServiceStub}: clientseitiger Stub, schickt jeden Aufruf als Nachricht an den Broker.</li>
 * </ul>
 * Jede Methode hat höchstens ein Argument und liefert nichts zurück. Verteilt wartet der Client nicht
 * (fire-and-forget): Den neuen Zustand erfahren alle über {@link GameListener#onStateChanged}, einen Fehler nur der
 * Absender über {@link GameListener#onError}. Lokal wirft {@link tictactoe.game.Game} bei einem Regelverstoß eine
 * {@link GameException}.
 */
public interface GameService {
    /**
     * Meldet einen Spieler an; das Symbol (X oder O) vergibt das Spiel.
     *
     * @param player anzumeldender Spieler, ausgewertet wird nur der Name
     */
    void join(Player player);

    /**
     * Führt einen Zug aus, wenn er nach den Spielregeln erlaubt ist.
     *
     * @param move Spieler, Zeile und Spalte (jeweils 0 bis 2)
     */
    void makeMove(Move move);

    /** Startet die Runde neu (auch nach Spielende); die Spieler bleiben angemeldet. */
    void reset();

    /**
     * Meldet einen Spieler ab; sein Platz wird frei.
     *
     * @param player abzumeldender Spieler, ausgewertet wird nur der Name
     */
    void leave(Player player);
}
