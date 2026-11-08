package tictactoe.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Liest einen Zug tolerant ein: "1 2", "12", "1,2", "1;2" -> {zeile, spalte}. */
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
}
