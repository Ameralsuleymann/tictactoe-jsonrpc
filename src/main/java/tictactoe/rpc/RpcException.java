package tictactoe.rpc;

/**
 * Technischer Fehler der RPC-Schicht, im Gegensatz zur fachlichen {@link tictactoe.api.GameException}.
 *
 * <p>Typische Ursachen:
 * <ul>
 *   <li>Verbindung verloren oder Senden fehlgeschlagen,</li>
 *   <li>keine Antwort innerhalb des Timeouts,</li>
 *   <li>unbekannter Dienst oder unbekannte Methode,</li>
 *   <li>entfernte Exception, die nicht in der Methodensignatur deklariert ist und deshalb beim Aufrufer nicht
 *       originalgetreu neu erzeugt werden kann.</li>
 * </ul>
 * Als {@link RuntimeException} muss sie nicht in den Contract-Interfaces deklariert werden; diese bleiben dadurch
 * frei von technischen Details. Netzfehler bleiben fuer den Aufrufer aber sichtbar (keine Fehlertransparenz).
 */
public class RpcException extends RuntimeException {
    /**
     * Erzeugt eine RPC-Exception mit Fehlermeldung.
     *
     * @param message Beschreibung des Fehlers
     */
    public RpcException(String message) { super(message); }

    /**
     * Erzeugt eine RPC-Exception mit Fehlermeldung und Ursache.
     *
     * @param message Beschreibung des Fehlers
     * @param cause   urspruengliche Exception
     */
    public RpcException(String message, Throwable cause) { super(message, cause); }
}
