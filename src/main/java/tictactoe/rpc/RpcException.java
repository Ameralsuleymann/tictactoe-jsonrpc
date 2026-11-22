package tictactoe.rpc;

/**
 * Technischer Fehler der RPC-Schicht, im Gegensatz zur fachlichen {@link tictactoe.api.GameException}.
 *
 * <p>Ursachen: keine Antwort innerhalb des Timeouts, Verbindung verloren, unbekannte Methode oder ein Fehler auf der
 * Gegenseite, der keine GameException ist. Als {@link RuntimeException} muss sie nicht in den Contract-Interfaces
 * stehen; diese bleiben frei von technischen Details.
 */
public class RpcException extends RuntimeException {
    /**
     * Erzeugt eine RPC-Exception.
     *
     * @param message Beschreibung des Fehlers
     */
    public RpcException(String message) { super(message); }
}
