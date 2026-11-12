package tictactoe.api;

import tictactoe.model.GameState;
import tictactoe.model.Move;
import tictactoe.model.Player;

/**
 * Fassade zum Spiel; in der verteilten Version der C-S-Contract in Richtung Client &rarr; Server.
 *
 * <p>Implementierungen:
 * <ul>
 *   <li>{@link tictactoe.game.Game} &ndash; die Spiellogik selbst (lokal im Prozess oder auf dem Server),</li>
 *   <li>{@link tictactoe.server.GameSession} &ndash; serverseitiger Endpunkt je Client, delegiert an {@code Game},</li>
 *   <li>der Stub aus {@link tictactoe.rpc.RpcPeer#proxy(String, Class)} &ndash; leitet Aufrufe per JSON-RPC
 *       an den Server weiter.</li>
 * </ul>
 * Da lokale und entfernte Aufrufe ueber dieselbe Schnittstelle laufen, ist Zugriffstransparenz gegeben.
 * Parameter und Rueckgabewerte sind DTOs aus {@link tictactoe.model} und werden per Wert (als Kopie) uebertragen.
 *
 * @see GameListener
 */
public interface GameService {
    /**
     * Meldet einen Spieler an; liefert ihn mit zugewiesenem Symbol zurueck.
     *
     * <p>Der Spieler erhaelt das freie Symbol (zuerst {@link tictactoe.model.Symbol#X}, dann
     * {@link tictactoe.model.Symbol#O}). Mit dem zweiten Spieler beginnt die Runde.
     *
     * @param player anzumeldender Spieler; ausgewertet wird nur der Name
     * @return neue Instanz des Spielers mit zugewiesenem Symbol
     * @throws GameException wenn der Name fehlt oder schon vergeben ist oder bereits zwei Spieler angemeldet sind
     */
    Player join(Player player) throws GameException;

    /**
     * Fuehrt einen Zug aus, nachdem alle Spielregeln geprueft wurden.
     *
     * @param move der Zug (Spieler, Zeile und Spalte jeweils 0 bis 2)
     * @return der Spielzustand nach dem Zug
     * @throws GameException wenn das Spiel nicht laeuft, der Spieler unbekannt oder nicht am Zug ist oder das Feld
     *                       ausserhalb des Spielfelds liegt bzw. bereits belegt ist
     */
    GameState makeMove(Move move) throws GameException;

    /**
     * Liefert den aktuellen Spielzustand.
     *
     * @return Kopie des aktuellen Spielzustands
     */
    GameState getState();

    /**
     * Startet die laufende Runde neu oder beginnt nach Spielende eine neue Runde; die Spieler bleiben angemeldet.
     */
    void reset();
}
