package tictactoe.model;

/**
 * DTO: Spieler mit Name und Symbol, unveraenderlich (Record), wird in beide Richtungen uebertragen.
 *
 * <p>Client &rarr; Server bei {@link tictactoe.api.GameService#join(Player)} und als Teil von {@link Move};
 * Server &rarr; Client als Rueckgabewert von {@code join}, in den Callbacks von {@link tictactoe.api.GameListener}
 * und als Teil von {@link GameState}. Spieler werden ueber ihren Namen identifiziert.
 *
 * <p>Wie alle DTOs ist {@code Player} unveraenderlich: Uebertragen wird immer eine Kopie, eine Aenderung auf der
 * einen Seite haette auf der anderen ohnehin keine Wirkung. Das Symbol vergibt der Server, indem er beim Beitritt
 * einen neuen {@code Player} mit Symbol erzeugt.
 *
 * @param name   eindeutiger Name des Spielers
 * @param symbol Spielstein des Spielers; {@code null}, solange der Spieler noch nicht beigetreten ist
 */
public record Player(String name, Symbol symbol) {
    /**
     * Erzeugt einen Spieler ohne Symbol, etwa fuer die Anmeldung mit {@link tictactoe.api.GameService#join(Player)}.
     *
     * @param name Name des Spielers
     */
    public Player(String name) { this(name, null); }

    /**
     * Textdarstellung fuer die Konsole.
     *
     * @return Name, gefolgt vom Symbol in Klammern, falls vorhanden, z.B. {@code Alice (X)}
     */
    @Override public String toString() { return name + (symbol != null ? " (" + symbol + ")" : ""); }
}
