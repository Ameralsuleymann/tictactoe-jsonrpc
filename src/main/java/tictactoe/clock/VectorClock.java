package tictactoe.clock;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

/**
 * Vektoruhr nach Mattern, wie in der Vorlesung (VS_03, Folien 23-25): ein Zähler je Prozess.
 *
 * <p>Jeder Prozess (Server und jeder Client) hat eine eigene Uhr. Prozesse werden über ihren Namen identifiziert
 * ({@code Server}, {@code Alice}, {@code Bob}); ein fehlender Eintrag zählt als 0.
 * <ul>
 *   <li><b>Senden</b> ({@link #tickForSend()}): eigenen Eintrag um 1 erhöhen, eine Kopie der Uhr wird als Zeitstempel
 *       mit der Nachricht verschickt.</li>
 *   <li><b>Empfangen</b> ({@link #receive(VectorClock)}): eigenen Eintrag um 1 erhöhen, dann komponentenweise das
 *       Maximum mit dem Zeitstempel der Nachricht bilden.</li>
 * </ul>
 * Vergleich zweier Zeitstempel: a &le; b, wenn jede Komponente von a höchstens so groß ist wie in b. Gilt weder
 * a &le; b noch b &le; a, sind die Ereignisse nebenläufig (a || b): keins kann das andere beeinflusst haben.
 */
public class VectorClock {
    /** Name des Servers in jeder Uhr; er steht in der Ausgabe immer vorne. */
    public static final String SERVER = "Server";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() { }.getType();

    /** Prozess, dem die Uhr gehört; bei einem empfangenen Zeitstempel ohne Bedeutung. */
    private final String owner;
    /** Zähler je Prozessname, sortiert: Server zuerst, dann alphabetisch. */
    private final Map<String, Integer> counts = new TreeMap<>(VectorClock::compareNames);

    /**
     * Erzeugt die Uhr eines Prozesses; alle Einträge stehen auf 0.
     *
     * @param owner Name des Prozesses, z.B. {@code Server} oder {@code Alice}
     */
    public VectorClock(String owner) {
        this.owner = owner;
        counts.put(owner, 0);
    }

    private VectorClock(String owner, Map<String, Integer> counts) {
        this.owner = owner;
        this.counts.putAll(counts);
    }

    /**
     * Sendeereignis: eigenen Eintrag erhöhen.
     *
     * @return Kopie der Uhr als Zeitstempel für die Nachricht
     */
    public synchronized VectorClock tickForSend() {
        counts.merge(owner, 1, Integer::sum);
        return copy();
    }

    /**
     * Empfangsereignis: eigenen Eintrag erhöhen, dann komponentenweise Maximum mit dem Zeitstempel.
     *
     * @param stamp Zeitstempel der empfangenen Nachricht
     */
    public synchronized void receive(VectorClock stamp) {
        counts.merge(owner, 1, Integer::sum);
        stamp.counts().forEach((name, n) -> counts.merge(name, n, Math::max));
    }

    /**
     * Liefert eine unabhängige Kopie, z.B. als Zeitstempel eines Spielzustands.
     *
     * @return Kopie dieser Uhr
     */
    public synchronized VectorClock copy() { return new VectorClock(owner, counts); }

    /**
     * Prüft a &le; b: Jede Komponente dieser Uhr ist höchstens so groß wie in {@code other}.
     *
     * @param other Vergleichsuhr
     * @return {@code true}, wenn dieses Ereignis vor {@code other} liegt oder gleich ist
     */
    public boolean isBeforeOrEqual(VectorClock other) {
        Map<String, Integer> mine = counts();
        Map<String, Integer> theirs = other.counts();
        for (Map.Entry<String, Integer> e : mine.entrySet())
            if (e.getValue() > theirs.getOrDefault(e.getKey(), 0)) return false;
        return true;
    }

    /**
     * Beziehung zu einer anderen Uhr für die Ausgabe.
     *
     * @param other Vergleichsuhr
     * @return {@code "="}, {@code "<"}, {@code ">"} oder {@code "||"} (nebenläufig)
     */
    public String relationTo(VectorClock other) {
        boolean before = isBeforeOrEqual(other);
        boolean after = other.isBeforeOrEqual(this);
        if (before && after) return "=";
        if (before) return "<";
        if (after) return ">";
        return "||";
    }

    /**
     * Wandelt die Uhr in JSON um, z.B. {@code {"Server":5,"Alice":4,"Bob":2}} (JMS-Property einer Nachricht).
     *
     * @return JSON-Text
     */
    public String toJson() { return GSON.toJson(counts()); }

    /**
     * Liest einen Zeitstempel aus JSON.
     *
     * @param json JSON-Text aus der JMS-Property
     * @return der Zeitstempel
     */
    public static VectorClock fromJson(String json) { return new VectorClock(null, GSON.fromJson(json, MAP_TYPE)); }

    /** @return z.B. {@code [Server=5, Alice=4, Bob=2]} */
    @Override public synchronized String toString() {
        String s = counts.toString();
        return "[" + s.substring(1, s.length() - 1) + "]";
    }

    private synchronized Map<String, Integer> counts() { return new TreeMap<>(counts); }

    private static int compareNames(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.equals(SERVER)) return -1;
        if (b.equals(SERVER)) return 1;
        return a.compareTo(b);
    }
}
