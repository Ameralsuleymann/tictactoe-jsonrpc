package tictactoe.server;

import java.net.*;
import java.util.Collections;
import tictactoe.api.GameService;
import tictactoe.game.Game;
import tictactoe.rpc.RpcPeer;

/**
 * Start: java -cp ... tictactoe.server.GameServer [port]   (-Drpc.verbose=true zeigt die JSON-Nachrichten)
 * Laeuft dauerhaft: nach Spielende geht es mit 'neu' weiter, getrennte Clients geben ihren Platz frei.
 */
public class GameServer {
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

    /** IPv4-Adressen dieses Rechners, die Clients bei "Server-IP" eingeben koennen (z.B. Hamachi 25.x.x.x). */
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
