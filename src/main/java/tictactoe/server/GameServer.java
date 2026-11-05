package tictactoe.server;

import java.net.ServerSocket;
import java.net.Socket;
import tictactoe.api.GameService;
import tictactoe.game.Game;
import tictactoe.rpc.RpcPeer;

/** Start: java -cp ... tictactoe.server.GameServer [port]   (-Drpc.verbose=true zeigt die JSON-Nachrichten) */
public class GameServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        boolean verbose = Boolean.getBoolean("rpc.verbose");
        Game game = new Game();
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("TicTacToe-Server lauscht auf Port " + port);
            while (true) {
                Socket socket = server.accept();
                System.out.println("Client verbunden: " + socket.getRemoteSocketAddress());
                RpcPeer peer = new RpcPeer(socket, verbose);
                peer.register("game", GameService.class, new GameSession(game, peer));
                peer.start();
            }
        }
    }
}
