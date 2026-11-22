package tictactoe.api;

import tictactoe.model.Move;
import tictactoe.model.Player;

/**
 * Contract Client &rarr; Server: alles, was ein Spieler am Spiel tun kann.
 *
 * <p>Implementierungen:
 * <ul>
 *   <li>{@link tictactoe.game.Game}: die Spiellogik (lokal oder auf dem Server),</li>
 *   <li>{@link tictactoe.server.GameSession}: serverseitig ein Objekt pro Client, leitet an {@code Game} weiter,</li>
 *   <li>{@link tictactoe.rpc.GameServiceStub}: clientseitiger Stub, schickt jeden Aufruf zum Server.</li>
 * </ul>
 * Jede Methode hat höchstens ein Argument und liefert nichts zurück. Den neuen Spielzustand erfahren die Clients
 * ausschließlich über {@link GameListener#onStateChanged}.
 */
public interface GameService {
    /**
     * Meldet einen Spieler an; das Symbol (X oder O) vergibt das Spiel.
     *
     * @param player anzumeldender Spieler, ausgewertet wird nur der Name
     * @throws GameException wenn der Name fehlt oder vergeben ist oder schon zwei Spieler angemeldet sind
     */
    void join(Player player) throws GameException;

    /**
     * Führt einen Zug aus, wenn er nach den Spielregeln erlaubt ist.
     *
     * @param move Spieler, Zeile und Spalte (jeweils 0 bis 2)
     * @throws GameException wenn die Runde nicht läuft, der Spieler nicht am Zug ist oder das Feld ungültig oder
     *                       belegt ist
     */
    void makeMove(Move move) throws GameException;

    /**
     * Startet die Runde neu (auch nach Spielende); die Spieler bleiben angemeldet.
     *
     * @throws GameException wenn noch keine zwei Spieler angemeldet sind
     */
    void reset() throws GameException;
}
