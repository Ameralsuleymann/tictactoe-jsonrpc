package tictactoe.server;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import tictactoe.game.Game;
import tictactoe.rpc.*;

/**
 * Startklasse des Servers der verteilten Version.
 *
 * <p>Ein {@link Game} für alle Clients. Pro Verbindung entstehen ein {@link RpcPeer}, ein {@link GameListenerStub}
 * (Server &rarr; Client), eine {@link GameSession} und ein {@link GameServiceSkeleton} (Client &rarr; Server).
 *
 * <p>Start: {@code GameServer [port]}; mit {@code -Drpc.verbose=true} werden alle JSON-Nachrichten angezeigt.
 */
public class GameServer {
    /**
     * Startet den Server und nimmt in einer Endlosschleife Verbindungen an.
     *
     * @param args optional {@code [port]}, Standard ist 5000
     * @throws IOException wenn der Port nicht geöffnet werden kann
     */
    public static void main(String[] args) throws IOException {
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
                GameSession session = new GameSession(game, new GameListenerStub(peer));  // Server -> Client
                peer.setSkeleton(new GameServiceSkeleton(session));                        // Client -> Server
                peer.onClose(session::leave);
                peer.start();
            }
        }
    }

    /** Gibt die IPv4-Adressen dieses Rechners aus, die Clients als Server-IP eingeben können. */
    private static void printAddresses() throws SocketException {
        System.out.println("Erreichbar unter:");
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) continue;
            for (InetAddress a : Collections.list(ni.getInetAddresses()))
                if (a instanceof Inet4Address) System.out.println("  " + a.getHostAddress() + "  (" + ni.getDisplayName() + ")");
        }
    }
}
