package tictactoe.local;

import java.util.Scanner;
import tictactoe.api.GameException;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.ui.ConsoleListener;
import tictactoe.ui.MoveInput;

/** Nicht verteilte Version: gleiche Klassen (Game, DTOs, ConsoleListener), kein Netzwerk. */
public class LocalMain {
    public static void main(String[] args) throws Exception {
        Game game = new Game();
        game.addListener(new ConsoleListener());
        game.join(new Player("Alice"));
        game.join(new Player("Bob"));

        Scanner in = new Scanner(System.in);
        while (game.getState().getStatus() == Status.RUNNING && in.hasNextLine()) {
            GameState s = game.getState();
            System.out.print(s.currentPlayer().getName() + " (zeile spalte): ");
            int[] rc = MoveInput.parse(in.nextLine());
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
