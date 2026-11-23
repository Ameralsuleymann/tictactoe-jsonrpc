package tictactoe.api;

import tictactoe.model.GameState;

/**
 * Contract Server &rarr; Client (Callback): Observer-Pattern in der Push-Variante.
 *
 * <p>Implementierungen:
 * <ul>
 *   <li>{@link tictactoe.ui.ConsoleListener}: zeigt Zustand und Fehler auf der Konsole an,</li>
 *   <li>{@link tictactoe.rpc.GameListenerStub}: serverseitiger Stub, schickt die Nachricht an den Broker,</li>
 *   <li>{@link tictactoe.server.LagCompensation}: merkt sich auf dem Server jeden Zustand fürs Zurückspulen.</li>
 * </ul>
 */
public interface GameListener {
    /**
     * Wird nach jeder Zustandsänderung aufgerufen (Beitritt, Zug, Neustart, Abmeldung); verteilt an alle Clients.
     *
     * @param state der vollständige neue Spielzustand
     */
    void onStateChanged(GameState state);

    /**
     * Meldet einen fachlichen Fehler, verteilt nur an den Client, dessen Aufruf gescheitert ist.
     * Standard: nichts tun, für Beobachter, die keine Fehler anzeigen (z.B. die Zeitleiste auf dem Server).
     *
     * @param message lesbare Fehlerbeschreibung, z.B. "Feld ist bereits belegt"
     */
    default void onError(String message) { }
}
