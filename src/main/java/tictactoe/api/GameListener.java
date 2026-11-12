package tictactoe.api;

import tictactoe.model.GameState;
import tictactoe.model.Player;

/**
 * Observer-Schnittstelle fuer Spielereignisse; in der verteilten Version der C-S-Contract in Richtung
 * Server &rarr; Client (Callback).
 *
 * <p>Lokal meldet sich ein Listener direkt bei {@link tictactoe.game.Game#addListener(GameListener)} an.
 * Verteilt registriert der Client seine Implementierung ({@link tictactoe.ui.ConsoleListener}) unter dem
 * Dienstnamen {@code "listener"} bei seinem {@link tictactoe.rpc.RpcPeer}; der Server erhaelt dafuer einen Stub.
 *
 * <p>Alle Methoden haben den Rueckgabetyp {@code void} und werden deshalb als JSON-RPC-Notification
 * (ohne {@code id}, ohne Antwort, fire-and-forget) uebertragen.
 */
public interface GameListener {
    /**
     * Wird aufgerufen, nachdem ein Spieler erfolgreich beigetreten ist.
     *
     * @param player der beigetretene Spieler inklusive zugewiesenem {@link tictactoe.model.Symbol}
     */
    void onPlayerJoined(Player player);

    /**
     * Wird aufgerufen, nachdem ein Spieler das Spiel verlassen hat (auf dem Server: Verbindung getrennt).
     *
     * @param player der Spieler, der das Spiel verlassen hat
     */
    void onPlayerLeft(Player player);

    /**
     * Wird nach jeder Zustandsaenderung aufgerufen (Beitritt, Zug, Neustart, Abmeldung).
     *
     * @param state Kopie des vollstaendigen, aktuellen Spielzustands
     */
    void onStateChanged(GameState state);
}
