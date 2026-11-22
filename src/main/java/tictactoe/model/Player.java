package tictactoe.model;

/**
 * DTO: Spieler mit Name und Symbol, unveränderlich (Record); wird in beide Richtungen übertragen.
 *
 * <p>Client &rarr; Server bei {@link tictactoe.api.GameService#join(Player)} und als Teil von {@link Move},
 * Server &rarr; Client als Teil von {@link GameState}. Spieler werden über ihren Namen identifiziert; das Symbol
 * vergibt der Server beim Beitritt, indem er einen neuen {@code Player} mit Symbol anlegt.
 *
 * @param name   eindeutiger Name
 * @param symbol Spielstein; {@code null}, solange der Server noch keinen vergeben hat
 */
public record Player(String name, Symbol symbol) {
    /**
     * Erzeugt einen Spieler ohne Symbol, so wie der Client ihn kennt.
     *
     * @param name Name des Spielers
     */
    public Player(String name) { this(name, null); }

    /**
     * Textdarstellung für die Konsole.
     *
     * @return z.B. {@code Alice (X)}, ohne Symbol nur der Name
     */
    @Override public String toString() { return symbol == null ? name : name + " (" + symbol + ")"; }
}
