package tictactoe.rpc;

/** Technischer RPC-Fehler (Verbindung weg, Timeout, unbekannte Methode, nicht deklarierte Remote-Exception). */
public class RpcException extends RuntimeException {
    public RpcException(String message) { super(message); }
    public RpcException(String message, Throwable cause) { super(message, cause); }
}
