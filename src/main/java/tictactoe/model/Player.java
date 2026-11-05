package tictactoe.model;

/** DTO: Spieler (Client -> Server bei join/makeMove, Server -> Client in Callbacks). */
public class Player {
    private String name;
    private Symbol symbol; // wird vom Server beim join vergeben

    public Player() { }
    public Player(String name) { this.name = name; }
    public Player(String name, Symbol symbol) { this.name = name; this.symbol = symbol; }

    public String getName() { return name; }
    public Symbol getSymbol() { return symbol; }
    public void setSymbol(Symbol symbol) { this.symbol = symbol; }

    @Override public String toString() { return name + (symbol != null ? " (" + symbol + ")" : ""); }
}
