package tictactoe.api;

/**
 * Fachlicher Fehler des Spiels, z.B. ein belegtes Feld oder ein Spieler, der nicht am Zug ist.
 *
 * <p>Checked Exception, deklariert in allen Methoden von {@link GameService}. Verteilt schickt der Server sie als
 * JSON-RPC-{@code error} mit dem Klassennamen; der {@link tictactoe.rpc.GameServiceStub} erzeugt sie beim Client neu
 * und wirft sie dort.
 *
 * @see tictactoe.rpc.RpcException
 */
public class GameException extends Exception {
    /**
     * Erzeugt einen fachlichen Fehler.
     *
     * @param message lesbare Fehlerbeschreibung; wird unverändert zum Client übertragen
     */
    public GameException(String message) { super(message); }
}
