package tictactoe.local;

import java.util.Scanner;
import tictactoe.api.GameException;
import tictactoe.game.Game;
import tictactoe.model.*;
import tictactoe.ui.ConsoleListener;

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
            try {
                String[] p = in.nextLine().trim().split("\\s+");
                game.makeMove(new Move(s.currentPlayer(), Integer.parseInt(p[0]), Integer.parseInt(p[1])));
            } catch (GameException e) {
                System.out.println("Fehler: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ungueltige Eingabe.");
            }
        }
    }
}
