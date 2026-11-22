package tictactoe.api;

import tictactoe.model.GameState;

/**
 * Contract Server &rarr; Client (Callback): Observer-Pattern in der Push-Variante.
 *
 * <p>Implementierungen:
 * <ul>
 *   <li>{@link tictactoe.ui.ConsoleListener}: zeigt den Zustand auf der Konsole an,</li>
 *   <li>{@link tictactoe.rpc.GameListenerStub}: serverseitiger Stub, schickt den Zustand zum Client.</li>
 * </ul>
 * Die Methode ist {@code void} und wird deshalb als JSON-RPC-Notification ohne Antwort übertragen.
 */
public interface GameListener {
    /**
     * Wird nach jeder Zustandsänderung aufgerufen (Beitritt, Zug, Neustart, Abmeldung).
     *
     * @param state der vollständige neue Spielzustand
     */
    void onStateChanged(GameState state);
}
