package tictactoe.api;

/**
 * Fachlicher Fehler des Spiels, z.B. ein belegtes Feld oder ein Spieler, der nicht am Zug ist.
 *
 * <p>Unchecked, weil der Contract fire-and-forget ist und keine {@code throws} hat. Lokal fängt
 * {@link tictactoe.local.LocalMain} sie direkt. Verteilt fängt sie der {@link tictactoe.rpc.GameServiceSkeleton} und
 * schickt die Meldung als {@link GameListener#onError} an den Absender.
 */
public class GameException extends RuntimeException {
    /**
     * Erzeugt einen fachlichen Fehler.
     *
     * @param message lesbare Fehlerbeschreibung; wird unverändert zum Client übertragen
     */
    public GameException(String message) { super(message); }
}
