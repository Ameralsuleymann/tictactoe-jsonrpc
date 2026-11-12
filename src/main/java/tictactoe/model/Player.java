package tictactoe.model;

/**
 * DTO: Spieler mit Name und Symbol, wird in beide Richtungen uebertragen.
 *
 * <p>Client &rarr; Server bei {@link tictactoe.api.GameService#join(Player)} und als Teil von {@link Move};
 * Server &rarr; Client als Rueckgabewert von {@code join}, in den Callbacks von {@link tictactoe.api.GameListener}
 * und als Teil von {@link GameState}. Spieler werden ueber ihren Namen identifiziert.
 */
public class Player {
    /** Eindeutiger Name des Spielers. */
    private String name;
    /** Spielstein des Spielers; vor dem Beitritt {@code null}. */
    private Symbol symbol; // wird vom Server beim join vergeben

    /** Erzeugt einen leeren Spieler; wird von Gson beim Unmarshalling verwendet. */
    public Player() { }

    /**
     * Erzeugt einen Spieler ohne Symbol, etwa fuer die Anmeldung mit {@link tictactoe.api.GameService#join(Player)}.
     *
     * @param name Name des Spielers
     */
    public Player(String name) { this.name = name; }

    /**
     * Erzeugt einen Spieler mit Symbol.
     *
     * @param name   Name des Spielers
     * @param symbol zugewiesener Spielstein
     */
    public Player(String name, Symbol symbol) { this.name = name; this.symbol = symbol; }

    /**
     * Liefert den Namen des Spielers.
     *
     * @return Name des Spielers
     */
    public String getName() { return name; }

    /**
     * Liefert den Spielstein des Spielers.
     *
     * @return zugewiesenes Symbol oder {@code null}, solange der Spieler nicht beigetreten ist
     */
    public Symbol getSymbol() { return symbol; }

    /**
     * Setzt den Spielstein des Spielers.
     *
     * @param symbol zuzuweisendes Symbol
     */
    public void setSymbol(Symbol symbol) { this.symbol = symbol; }

    /**
     * Textdarstellung fuer die Konsole.
     *
     * @return Name, gefolgt vom Symbol in Klammern, falls vorhanden, z.B. {@code Alice (X)}
     */
    @Override public String toString() { return name + (symbol != null ? " (" + symbol + ")" : ""); }
}
