package tictactoe.server;

import java.net.*;
import java.util.Collections;
import tictactoe.api.GameService;
import tictactoe.game.Game;
import tictactoe.rpc.RpcPeer;

/**
 * Startklasse des Servers der verteilten Version (Phase 2).
 *
 * <p>Lauscht auf einem TCP-Port und erzeugt fuer jeden verbundenen Client einen eigenen {@link RpcPeer} mit einer
 * {@link GameSession}, die als Dienst {@code "game"} registriert wird. Alle Sessions teilen sich eine
 * {@link Game}-Instanz.
 *
 * <p>Start: {@code java -cp ... tictactoe.server.GameServer [port]}; mit {@code -Drpc.verbose=true} werden die
 * JSON-Nachrichten angezeigt.<br>
 * Laeuft dauerhaft: nach Spielende geht es mit {@code neu} weiter, getrennte Clients geben ihren Platz frei.
 */
public class GameServer {
    /** Standardkonstruktor; der Server wird ueber {@link #main(String[])} gestartet. */
    public GameServer() { }

    /**
     * Startet den Server, gibt seine erreichbaren Adressen aus und nimmt in einer Endlosschleife Verbindungen an.
     *
     * @param args optional {@code [port]}, Standard ist {@code 5000}
     * @throws Exception wenn der Port nicht geoeffnet werden kann oder ein Ein-/Ausgabefehler auftritt
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        boolean verbose = Boolean.getBoolean("rpc.verbose");
        Game game = new Game();
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("TicTacToe-Server lauscht auf Port " + port + " (beenden mit Strg+C)");
            printAddresses();
            while (true) {
                Socket socket = server.accept();
                System.out.println("Client verbunden: " + socket.getRemoteSocketAddress());
                RpcPeer peer = new RpcPeer(socket, verbose);
                peer.register("game", GameService.class, new GameSession(game, peer));
                peer.start();
            }
        }
    }

    /**
     * Gibt die IPv4-Adressen dieses Rechners aus, die Clients bei "Server-IP" eingeben koennen.
     * Beruecksichtigt werden alle aktiven Netzwerkschnittstellen ohne Loopback, z.B. Hamachi mit {@code 25.x.x.x}.
     */
    private static void printAddresses() {
        try {
            System.out.println("Erreichbar unter:");
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address) System.out.println("  " + a.getHostAddress() + "  (" + ni.getDisplayName() + ")");
            }
        } catch (SocketException e) {
            System.out.println("  (Adressen nicht ermittelbar: " + e.getMessage() + ")");
        }
    }
}
