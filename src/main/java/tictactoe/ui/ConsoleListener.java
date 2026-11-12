package tictactoe.ui;

import tictactoe.api.GameListener;
import tictactoe.model.*;

/**
 * Konsolenanzeige des Spiels als Beobachter ({@link GameListener}).
 *
 * <p>Lokal ({@link tictactoe.local.LocalMain}) wird sie direkt bei {@link tictactoe.game.Game} angemeldet. Im Client
 * ({@link tictactoe.client.GameClient}) wird sie beim {@link tictactoe.rpc.RpcPeer} als Dienst {@code "listener"}
 * registriert und vom Server per RPC-Callback aufgerufen. Ausgegeben werden Beitritte, Abmeldungen und nach jeder
 * Zustandsaenderung das Spielfeld.
 */
public class ConsoleListener implements GameListener {
    /** Zuletzt angekuendigte Runde, damit die Ueberschrift einer Runde nur einmal erscheint. */
    private int shownRound;

    /** Erzeugt eine Konsolenanzeige; die erste Rundenueberschrift erscheint, sobald eine Runde laeuft. */
    public ConsoleListener() { }

    /**
     * Gibt aus, dass ein Spieler beigetreten ist.
     *
     * @param p der beigetretene Spieler
     */
    @Override public void onPlayerJoined(Player p) { System.out.println(">> Spieler beigetreten: " + p); }

    /**
     * Gibt aus, dass ein Spieler das Spiel verlassen hat.
     *
     * @param p der Spieler, der das Spiel verlassen hat
     */
    @Override public void onPlayerLeft(Player p) { System.out.println("<< Spieler hat das Spiel verlassen: " + p); }

    /**
     * Gibt das Spielfeld und den Status aus: wer am Zug ist, den Gewinner oder ein Unentschieden.
     * Zu Beginn einer neuen laufenden Runde erscheint einmalig die Rundenueberschrift, nach Spielende ein Hinweis
     * auf {@code neu} und {@code q}.
     *
     * @param s der neue Spielzustand
     */
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
