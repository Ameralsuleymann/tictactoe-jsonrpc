package tictactoe.local;

import java.util.Scanner;
import tictactoe.api.GameException;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/**
 * Startklasse der nicht verteilten Version: dieselben Klassen wie verteilt, aber ohne Netzwerk.
 *
 * <p>{@link Game} wird direkt aufgerufen und der {@link ConsoleListener} direkt als Beobachter angemeldet.
 * Alice (X) und Bob (O) spielen abwechselnd an einer Konsole.
 */
public class LocalMain {
    /** Letzter gemeldeter Zustand; daraus ergibt sich, wer gerade zieht. */
    private static GameState state;

    /**
     * Meldet Alice und Bob an und liest dann Züge und Befehle von der Konsole.
     *
     * @param args wird nicht ausgewertet
     * @throws GameException wenn die Anmeldung der beiden Spieler scheitert
     */
    public static void main(String[] args) throws GameException {
        Game game = new Game();
        game.addListener(new ConsoleListener());     // Anzeige
        game.addListener(s -> state = s);             // merkt sich den Zustand, wie jeder andere Beobachter
        game.join(new Player("Alice"));
        game.join(new Player("Bob"));
        System.out.println(MoveInput.HELP);

        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("q")) break;
            try {
                if (line.equalsIgnoreCase("neu")) {
                    game.reset();
                } else {
                    int[] rc = MoveInput.parse(line);
                    if (rc == null) System.out.println("Ungültige Eingabe. " + MoveInput.HELP);
                    else game.makeMove(new Move(state.currentPlayer(), rc[0], rc[1]));
                }
            } catch (GameException e) {
                System.out.println("Fehler: " + e.getMessage());
            }
        }
    }
}
