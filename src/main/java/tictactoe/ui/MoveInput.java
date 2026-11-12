package tictactoe.ui;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Konsoleneingabe: Zug tolerant lesen ({@code "1 2"}, {@code "12"}, {@code "1,2"}, {@code "1;2"}) und Befehle
 * ({@code neu}, {@code q}) erkennen.
 *
 * <p>Gemeinsam genutzt von {@link tictactoe.local.LocalMain} und {@link tictactoe.client.GameClient}. Die
 * Bereichspruefung der Koordinaten erfolgt nicht hier, sondern in {@link tictactoe.game.Game} (verteilt also auf
 * dem Server).
 */
public final class MoveInput {
    /** Eingabehinweis, der bei ungueltiger Eingabe angezeigt wird. */
    public static final String HINT = "Zeile und Spalte (0-2), z.B. 1 1 oder 11";

    /** Zwei Zahlen, getrennt durch Leerzeichen, Komma oder Semikolon, wie {@code 1 2} oder {@code 1,2}. */
    private static final Pattern TWO_NUMBERS = Pattern.compile("\\s*(\\d+)\\s*[\\s,;]\\s*(\\d+)\\s*");
    /** Zwei direkt aufeinanderfolgende Ziffern wie {@code 12}. */
    private static final Pattern TWO_DIGITS  = Pattern.compile("\\s*(\\d)(\\d)\\s*");

    /** Keine Instanzen; die Klasse bietet nur statische Methoden. */
    private MoveInput() { }

    /**
     * Liest einen Zug aus einer Eingabezeile.
     *
     * @param line Eingabezeile, darf {@code null} sein
     * @return {@code {zeile, spalte}} oder {@code null}, wenn die Eingabe kein Zug ist
     */
    public static int[] parse(String line) {
        if (line == null) return null;
        Matcher m = TWO_NUMBERS.matcher(line);
        if (!m.matches()) m = TWO_DIGITS.matcher(line);
        if (!m.matches()) return null;
        try {
            return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
        } catch (NumberFormatException e) {
            return null; // Zahl zu gross
        }
    }

    /**
     * Neustart der laufenden Runde oder neue Runde nach Spielende erkennen.
     * Erlaubt sind {@code neu}, {@code neustart}, {@code weiter} und {@code reset}, unabhaengig von Gross-/Kleinschreibung.
     *
     * @param line Eingabezeile, darf {@code null} sein
     * @return {@code true}, wenn die Eingabe ein Neustart-Befehl ist
     */
    public static boolean isRestart(String line) { return isOneOf(line, "neu", "neustart", "weiter", "reset"); }

    /**
     * Befehl zum Beenden erkennen.
     * Erlaubt sind {@code q}, {@code quit}, {@code exit} und {@code ende}, unabhaengig von Gross-/Kleinschreibung.
     *
     * @param line Eingabezeile, darf {@code null} sein
     * @return {@code true}, wenn die Eingabe ein Beenden-Befehl ist
     */
    public static boolean isQuit(String line) { return isOneOf(line, "q", "quit", "exit", "ende"); }

    /**
     * Prueft, ob die getrimmte Eingabe ohne Beachtung der Gross-/Kleinschreibung einem der Woerter entspricht.
     *
     * @param line  Eingabezeile, darf {@code null} sein
     * @param words erlaubte Woerter in Kleinbuchstaben
     * @return {@code true} bei Uebereinstimmung
     */
    private static boolean isOneOf(String line, String... words) {
        if (line == null) return false;
        String w = line.trim().toLowerCase(Locale.ROOT);
        for (String c : words) if (c.equals(w)) return true;
        return false;
    }
}
