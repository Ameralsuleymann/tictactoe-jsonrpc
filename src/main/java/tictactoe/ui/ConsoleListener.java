package tictactoe.ui;

import tictactoe.api.GameListener;
import tictactoe.model.*;

/** Konsolenanzeige. Wird lokal direkt und im Client als RPC-Callback-Empfaenger genutzt. */
public class ConsoleListener implements GameListener {
    private int shownRound;

    @Override public void onPlayerJoined(Player p) { System.out.println(">> Spieler beigetreten: " + p); }

    @Override public void onPlayerLeft(Player p) { System.out.println("<< Spieler hat das Spiel verlassen: " + p); }

    @Override public void onStateChanged(GameState s) {
        System.out.println();
        if (s.getStatus() == Status.RUNNING && s.getRound() != shownRound) {
            shownRound = s.getRound();
            System.out.println("===== Runde " + shownRound + " =====");
        }
        System.out.print(s.getBoard());
        switch (s.getStatus()) {
            case WAITING -> System.out.println("Warte auf zweiten Spieler ...");
            case RUNNING -> System.out.println("Am Zug: " + s.currentPlayer());
            case WON -> System.out.println("*** Gewinner: " + s.getWinner() + " ***");
            case DRAW -> System.out.println("*** Unentschieden ***");
        }
        if (s.getStatus() == Status.WON || s.getStatus() == Status.DRAW)
            System.out.println("Weiterspielen: 'neu'  |  Beenden: 'q'");
    }
}
