package tictactoe.rpc;

/**
 * Empfangsseite eines Remote-Interfaces: führt eine eingehende Nachricht auf dem echten Objekt aus.
 *
 * <p>Ein Skeleton wandelt den JSON-Body in ein DTO um (Unmarshalling) und ruft die Anwendungsklasse auf.
 * Implementierungen: {@link GameServiceSkeleton} (Server), {@link GameListenerSkeleton} (Client).
 */
public interface Skeleton {
    /**
     * Führt eine empfangene Nachricht aus.
     *
     * @param call Methode, Body, Zeitstempel und Antwortadresse der Nachricht
     */
    void invoke(Call call);
}
