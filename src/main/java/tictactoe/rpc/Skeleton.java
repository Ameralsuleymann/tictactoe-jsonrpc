package tictactoe.rpc;

import com.google.gson.JsonElement;

/**
 * Empfangsseite eines Remote-Interfaces: führt einen eingehenden Aufruf auf dem echten Objekt aus.
 *
 * <p>Ein Skeleton wandelt das JSON-Argument in ein DTO um (Unmarshalling), ruft die Anwendungsklasse auf und gibt das
 * Ergebnis als JSON zurück. Implementierungen: {@link GameServiceSkeleton} (Server), {@link GameListenerSkeleton}
 * (Client).
 */
public interface Skeleton {
    /**
     * Führt einen eingehenden Aufruf aus.
     *
     * @param method Methodenname aus der Nachricht
     * @param param  das Argument als JSON, {@code null} bei Methoden ohne Argument
     * @return Ergebnis als JSON, {@code null} bei {@code void}-Methoden
     * @throws Exception Fehler der Methode; der {@link RpcPeer} sendet ihn als JSON-RPC-{@code error} zurück
     */
    JsonElement invoke(String method, JsonElement param) throws Exception;
}
