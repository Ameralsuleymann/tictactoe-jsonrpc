package tictactoe.model;

import java.util.List;

/**
 * DTO: vollständiger Spielzustand, unveränderlich (Record).
 *
 * <p>Wird vom Server zum Client im Callback {@link tictactoe.api.GameListener#onStateChanged} übertragen und enthält
 * selbst die DTOs {@link Board} und {@link Player}. Einen eigenen Gewinner gibt es nicht: Nach dem Gewinnzug wechselt
 * das Zugrecht nicht mehr, bei {@link Status#WON} ist {@code current} also das Symbol des Gewinners.
 *
 * @param board   Spielfeld
 * @param players angemeldete Spieler (0 bis 2)
 * @param current Symbol des Spielers am Zug; bei {@link Status#WON} das Symbol des Gewinners
 * @param status  Status der Runde
 */
public record GameState(Board board, List<Player> players, Symbol current, Status status) {
    /**
     * Übernimmt die Spielerliste als unveränderliche Kopie.
     *
     * @param board   Spielfeld
     * @param players angemeldete Spieler
     * @param current Symbol des Spielers am Zug bzw. des Gewinners
     * @param status  Status der Runde
     */
    public GameState {
        players = List.copyOf(players);
    }

    /**
     * Liefert den Spieler zum Symbol {@link #current()}.
     *
     * @return Spieler am Zug, bei {@link Status#WON} der Gewinner; {@code null}, wenn niemand dieses Symbol hat
     */
    public Player currentPlayer() {
        for (Player p : players) if (p.symbol() == current) return p;
        return null;
    }
}
