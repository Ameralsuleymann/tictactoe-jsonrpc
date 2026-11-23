package tictactoe.server;

import java.util.ArrayList;
import java.util.List;
import tictactoe.api.GameException;
import tictactoe.api.GameListener;
import tictactoe.clock.VectorClock;
import tictactoe.game.Game;
import tictactoe.model.GameState;
import tictactoe.model.Move;

/**
 * Verzögerungskompensation durch serverseitiges Zurückspulen (Aufgabe 4, vgl. "Vector clocks in Simple Game").
 *
 * <p>Als {@link GameListener} am {@link Game} merkt sie sich nach jeder Zustandsänderung einen Snapshot: den Zustand
 * zusammen mit der Serveruhr. Kommt ein Zug an, sucht sie den jüngsten Snapshot, dessen Uhr &le; dem Zeitstempel des
 * Zugs ist. Das ist der Zustand, den der Spieler beim Ziehen gesehen hat.
 * <ul>
 *   <li>Ist das der letzte Snapshot, kannte der Spieler den aktuellen Zustand: Der Zug läuft normal.</li>
 *   <li>Sonst ist der Zug nebenläufig (||) zu den späteren Änderungen, er wurde auf einem älteren Zustand gemacht.
 *       Der Server spult auf diesen Zustand zurück, verwirft die späteren Änderungen und führt den Zug dort aus.
 *       Ist der Zug auch auf dem alten Zustand ungültig, bleibt alles, wie es war.</li>
 * </ul>
 */
public class LagCompensation implements GameListener {
    /** So viele Zustände werden höchstens aufbewahrt; ältere Züge lassen sich nicht mehr ausgleichen. */
    private static final int MAX_SNAPSHOTS = 20;

    /**
     * Ein gespeicherter Zustand. {@link GameState} ist ein unveränderlicher Record und damit schon ein fertiger Snapshot.
     *
     * @param clock Serveruhr, als der Zustand entstand
     * @param state der Zustand
     */
    private record Snapshot(VectorClock clock, GameState state) { }

    private final Game game;
    private final VectorClock serverClock;
    /** Zeitleiste der letzten Zustände, älteste zuerst. */
    private final List<Snapshot> history = new ArrayList<>();

    /**
     * Erzeugt die Kompensation; sie muss danach als erster Listener am Spiel angemeldet werden.
     *
     * @param game        das Spiel
     * @param serverClock die Vektoruhr des Servers
     */
    public LagCompensation(Game game, VectorClock serverClock) {
        this.game = game;
        this.serverClock = serverClock;
    }

    /** Snapshot mit der aktuellen Serveruhr speichern; läuft unter der Sperre von {@link Game}. */
    @Override
    public void onStateChanged(GameState state) {
        history.add(new Snapshot(serverClock.copy(), state));
        if (history.size() > MAX_SNAPSHOTS) history.remove(0);
    }

    /**
     * Führt einen Zug aus und spult vorher zurück, falls der Spieler den aktuellen Zustand noch nicht kannte.
     *
     * @param move  der Zug
     * @param stamp Vektoruhr-Zeitstempel der Nachricht mit dem Zug
     * @throws GameException wenn der Zug ungültig oder zu stark verzögert ist
     */
    public void makeMove(Move move, VectorClock stamp) {
        synchronized (game) {                                   // dieselbe Sperre wie Game: niemand ändert dazwischen
            int last = history.size() - 1;
            int seen = last;
            while (seen >= 0 && !history.get(seen).clock().isBeforeOrEqual(stamp)) seen--;
            if (seen == last) {                                 // Spieler kannte den aktuellen Zustand
                game.makeMove(move);
                return;
            }
            Snapshot latest = history.get(last);
            String who = move.player().name();
            System.out.println("[Zurückspulen] Zug von " + who + " mit " + stamp + " " + stamp.relationTo(latest.clock())
                    + " letzter Zustand " + latest.clock() + ": nebenläufig, " + who + " kannte ihn nicht");
            if (seen < 0) throw new GameException("Zug zu stark verzögert");

            Snapshot target = history.get(seen);
            List<Snapshot> later = new ArrayList<>(history.subList(seen + 1, history.size()));
            System.out.println("[Zurückspulen] zurück auf den Zustand " + target.clock() + ", den " + who
                    + " gesehen hat; " + later.size() + " spätere Änderung(en) verworfen");
            history.subList(seen + 1, history.size()).clear();
            game.restore(target.state());
            try {
                game.makeMove(move);
            } catch (GameException e) {                         // auch damals ungültig: alles wie vorher
                history.addAll(later);
                game.restore(latest.state());
                throw e;
            }
        }
    }
}
