package tictactoe.model;

import java.util.List;

/**
 * DTO: vollstaendiger Spielzustand, unveraenderlich (Record), vom Server zum Client als Rueckgabewert und in Callbacks.
 *
 * <p>Wird uebertragen als Rueckgabewert von {@link tictactoe.api.GameService#makeMove(Move)} und
 * {@link tictactoe.api.GameService#getState()} sowie im Callback
 * {@link tictactoe.api.GameListener#onStateChanged(GameState)}. Enthaelt selbst die DTOs {@link Board} und
 * {@link Player}. Der Client erhaelt immer den kompletten Zustand, keine Delta-Updates.
 *
 * <p>Alle Bestandteile sind unveraenderlich ({@link Board}, {@link Player}, die Spielerliste wird beim Erzeugen
 * kopiert). Ein {@code GameState} ist damit ein fester Schnappschuss, den {@link tictactoe.game.Game} ohne
 * zusaetzliche Kopien herausgeben kann.
 *
 * @param board   Spielfeld
 * @param players angemeldete Spieler (0 bis 2), als unveraenderliche Liste
 * @param current Symbol des Spielers, der am Zug ist
 * @param status  Status der Runde
 * @param winner  Gewinner der Runde oder {@code null}
 * @param round   Rundennummer, beginnend bei 1; zaehlt hoch bei Neustart oder neuer Runde
 */
public record GameState(Board board, List<Player> players, Symbol current, Status status, Player winner, int round) {
    /**
     * Prueft und uebernimmt die Bestandteile; die Spielerliste wird als unveraenderliche Kopie gespeichert.
     *
     * @param board   Spielfeld
     * @param players angemeldete Spieler; {@code null} wird als leere Liste uebernommen
     * @param current Symbol des Spielers, der am Zug ist
     * @param status  Status der Runde
     * @param winner  Gewinner oder {@code null}
     * @param round   Rundennummer
     */
    public GameState {
        players = players == null ? List.of() : List.copyOf(players);
    }

    /**
     * Ermittelt den Spieler, der am Zug ist.
     *
     * @return Spieler mit dem Symbol {@link #current()} oder {@code null}, wenn dieser nicht angemeldet ist
     */
    public Player currentPlayer() {
        for (Player p : players) if (p.symbol() == current) return p;
        return null;
    }
}
