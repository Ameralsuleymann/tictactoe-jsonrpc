package tictactoe.local;

import java.util.Scanner;
import tictactoe.api.GameException;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/**
 * Startklasse der nicht verteilten Version (Phase 1).
 *
 * <p>Verwendet dieselben Klassen wie die verteilte Version ({@link Game}, die DTOs aus {@link tictactoe.model} und
 * {@link ConsoleListener}), aber ohne Netzwerk: {@code Game} wird direkt aufgerufen und der {@code ConsoleListener}
 * direkt als Beobachter angemeldet. Zwei Spieler (Alice = X, Bob = O) spielen abwechselnd an einer Konsole.
 */
public class LocalMain {
    /** Standardkonstruktor; die lokale Version wird ueber {@link #main(String[])} gestartet. */
    public LocalMain() { }

    /**
     * Meldet Alice und Bob an und liest anschliessend Zuege und Befehle ({@code neu}, {@code q}) von der Konsole.
     * Fachliche Fehler ({@link GameException}) werden angezeigt, das Spiel laeuft weiter.
     *
     * @param args wird nicht ausgewertet
     * @throws Exception bei unerwarteten Fehlern, z.B. wenn die Anmeldung der beiden Spieler scheitert
     */
    public static void main(String[] args) throws Exception {
        Game game = new Game();
        game.addListener(new ConsoleListener());
        game.join(new Player("Alice"));
        game.join(new Player("Bob"));

        Scanner in = new Scanner(System.in);
        while (true) {
            GameState s = game.getState();
            boolean running = s.getStatus() == Status.RUNNING;
            System.out.print(running ? s.currentPlayer().getName() + " (zeile spalte | neu | q): " : "neu | q: ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine();
            if (MoveInput.isQuit(line)) break;
            if (MoveInput.isRestart(line)) { game.reset(); continue; }
            int[] rc = MoveInput.parse(line);
            if (rc == null) {
                System.out.println("Ungueltige Eingabe - " + MoveInput.HINT);
                continue;
            }
            try {
                game.makeMove(new Move(s.currentPlayer(), rc[0], rc[1]));
            } catch (GameException e) {
                System.out.println("Fehler: " + e.getMessage());
            }
        }
    }
}
