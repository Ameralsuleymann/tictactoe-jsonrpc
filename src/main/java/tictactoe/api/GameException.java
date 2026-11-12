package tictactoe.api;

/**
 * Fachlicher Fehler des Spiels, etwa ein ungueltiger Zug, ein Spieler, der nicht am Zug ist, oder ein volles Spiel.
 *
 * <p>Die Exception ist eine <em>checked</em> Exception und in den Methodensignaturen von {@link GameService}
 * deklariert. In der verteilten Version wird sie vom Server als JSON-RPC-{@code error} (mit dem Klassennamen in
 * {@code data.exception}) uebertragen. Der Client-Stub ({@link tictactoe.rpc.RpcPeer}) erkennt anhand der
 * Methodensignatur, dass sie deklariert ist, erzeugt sie neu und wirft sie beim Aufrufer.
 *
 * @see tictactoe.rpc.RpcException
 */
public class GameException extends Exception {
    /**
     * Erzeugt eine neue fachliche Exception.
     *
     * @param message fuer den Benutzer lesbare Fehlerbeschreibung; wird unveraendert zum Client uebertragen
     */
    public GameException(String message) { super(message); }
}
