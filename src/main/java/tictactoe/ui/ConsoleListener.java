package tictactoe.ui;

import java.util.stream.Collectors;
import tictactoe.api.GameListener;
import tictactoe.model.*;

/**
 * Konsolenanzeige des Spiels als Beobachter ({@link GameListener}).
 *
 * <p>Lokal ({@link tictactoe.local.LocalMain}) ruft {@link tictactoe.game.Game} sie direkt auf, im Client ruft der
 * Server sie über das {@link tictactoe.rpc.GameListenerSkeleton} auf. Ausgegeben werden Spielfeld, Spieler und Status.
 */
public class ConsoleListener implements GameListener {
    @Override
    public void onStateChanged(GameState s) {
        System.out.println();
        System.out.print(s.board());
        System.out.println("Spieler: " + s.players().stream().map(Player::toString).collect(Collectors.joining(", ")));
        switch (s.status()) {
            case WAITING -> System.out.println("Warte auf zweiten Spieler ...");
            case RUNNING -> System.out.println("Am Zug: " + s.currentPlayer());
            case WON -> System.out.println("Gewinner: " + s.currentPlayer() + "  ('neu' = neue Runde, 'q' = beenden)");
            case DRAW -> System.out.println("Unentschieden  ('neu' = neue Runde, 'q' = beenden)");
        }
    }
}
