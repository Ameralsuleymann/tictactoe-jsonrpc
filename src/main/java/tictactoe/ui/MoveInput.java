package tictactoe.ui;

/**
 * Konsoleneingabe für {@link tictactoe.local.LocalMain} und {@link tictactoe.client.GameClient}.
 * Ob ein Feld gültig ist, prüft erst {@link tictactoe.game.Game} (verteilt also der Server).
 */
public final class MoveInput {
    /** Hilfetext mit allen Befehlen. */
    public static final String HELP = "Befehle: 'zeile spalte' (z.B. 1 1 oder 11) | neu = neue Runde | q = beenden";

    private MoveInput() { }

    /**
     * Liest einen Zug: Alles außer Ziffern wird ignoriert, übrig bleiben müssen genau zwei Ziffern.
     * {@code "1 2"}, {@code "12"} und {@code "1,2"} ergeben Zeile 1, Spalte 2.
     *
     * @param line Eingabezeile
     * @return {@code {zeile, spalte}} oder {@code null}, wenn die Eingabe kein Zug ist
     */
    public static int[] parse(String line) {
        String digits = line.replaceAll("\\D", "");
        if (digits.length() != 2) return null;
        return new int[] { digits.charAt(0) - '0', digits.charAt(1) - '0' };
    }
}
