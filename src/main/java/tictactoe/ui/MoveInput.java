package tictactoe.ui;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Konsoleneingabe: Zug tolerant lesen ("1 2", "12", "1,2", "1;2") und Befehle (neu, q) erkennen. */
public final class MoveInput {
    public static final String HINT = "Zeile und Spalte (0-2), z.B. 1 1 oder 11";

    private static final Pattern TWO_NUMBERS = Pattern.compile("\\s*(\\d+)\\s*[\\s,;]\\s*(\\d+)\\s*");
    private static final Pattern TWO_DIGITS  = Pattern.compile("\\s*(\\d)(\\d)\\s*");

    private MoveInput() { }

    /** @return {zeile, spalte} oder null, wenn die Eingabe kein Zug ist */
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

    /** Neustart der laufenden Runde bzw. neue Runde nach Spielende. */
    public static boolean isRestart(String line) { return isOneOf(line, "neu", "neustart", "weiter", "reset"); }

    public static boolean isQuit(String line) { return isOneOf(line, "q", "quit", "exit", "ende"); }

    private static boolean isOneOf(String line, String... words) {
        if (line == null) return false;
        String w = line.trim().toLowerCase(Locale.ROOT);
        for (String c : words) if (c.equals(w)) return true;
        return false;
    }
}
