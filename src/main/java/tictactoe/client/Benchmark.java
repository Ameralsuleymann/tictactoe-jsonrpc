package tictactoe.client;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tictactoe.api.GameException;
import tictactoe.api.GameService;
import tictactoe.game.Game;
import tictactoe.model.GameState;
import tictactoe.model.Player;
import tictactoe.rpc.RpcException;
import tictactoe.rpc.RpcPeer;

/**
 * Zeitvergleich eines lokalen Funktionsaufrufs mit einem entfernten RPC-Aufruf (Liefergegenstand 6).
 *
 * <p>Gemessen wird {@link GameService#getState()}, einmal auf einem {@link Game} im selben Prozess und einmal ueber
 * den Stub zum Server. Beide Aufrufe laufen ueber dieselbe Schnittstelle; der einzige Unterschied ist der
 * Transportweg. Um die Remote-Zeit aufzuschluesseln, werden zusaetzlich ein leerer RPC-Aufruf
 * ({@link RpcPeer.Ping#ping()}) und das reine Marshalling des {@link GameState} ohne Netz gemessen.
 *
 * <p>Vorgehen:
 * <ul>
 *   <li>Warm-up vor jeder Messreihe, damit der JIT-Compiler den Code bereits optimiert hat,</li>
 *   <li>danach wird jeder Aufruf einzeln mit {@link System#nanoTime()} gemessen,</li>
 *   <li>Remote-Aufruf und leerer RPC-Aufruf werden abwechselnd gemessen, damit beide unter denselben Bedingungen
 *       laufen (JIT-Zustand, Netzlast, WLAN-Schwankungen),</li>
 *   <li>ausgegeben werden Minimum, Maximum, Mittelwert und Median in Mikrosekunden pro Aufruf.</li>
 * </ul>
 * Faktor und Aufschluesselung verwenden den Median, weil er robust gegen Ausreisser ist (Garbage Collection, WLAN).
 *
 * <p>Die drei Szenarien ergeben sich aus zwei Laeufen: Jeder Lauf misst lokal und remote ueber die aktuelle
 * Verbindung, einmal im Client auf dem Server-Rechner ({@code localhost}) und einmal im Client auf dem zweiten
 * Rechner (Netzwerk). Das Szenario wird automatisch erkannt und ausgegeben.
 *
 * <p>In der nicht verteilten Version ({@link tictactoe.local.LocalMain}) gibt es keinen Server; dort misst
 * {@link #runLocal(GameService, int)} nur den lokalen Aufruf und zum Vergleich das Marshalling ohne Netz.
 */
public final class Benchmark {
    /** Anzahl gemessener Aufrufe je Messreihe, wenn bei {@code bench} keine Anzahl angegeben ist. */
    public static final int DEFAULT_CALLS = 1000;
    /** Warm-up-Aufrufe vor lokalen Messungen; der JIT-Compiler optimiert erst nach einigen tausend Aufrufen. */
    private static final int WARMUP_LOCAL = 20_000;
    /** Warm-up-Aufrufe vor der Marshalling-Messung. */
    private static final int WARMUP_MARSHAL = 5_000;
    /** Warm-up-Aufrufe vor Remote-Messungen; weniger, da jeder Aufruf deutlich laenger dauert. */
    private static final int WARMUP_REMOTE = 500;
    /** Erkennt {@code bench} oder {@code bench <anzahl>}, unabhaengig von Gross-/Kleinschreibung. */
    private static final Pattern COMMAND = Pattern.compile("\\s*bench(?:\\s+(\\d{1,6}))?\\s*", Pattern.CASE_INSENSITIVE);
    /** Zeilenformat der Ergebnistabelle: Bezeichnung, Minimum, Maximum, Mittelwert, Median. */
    private static final String ROW = "%-42s %10.2f %10.2f %10.2f %10.2f%n";
    /** Zeilenformat der Aufschluesselung: Bezeichnung, Median in Mikrosekunden, Anteil in Prozent. */
    private static final String PART = "  %-52s %10.2f us %6.1f %%%n";

    /** Nimmt die Ergebnisse auf, damit der JIT-Compiler die gemessenen Aufrufe nicht als wirkungslos entfernt. */
    private static volatile Object sink;

    /** Keine Instanzen; die Klasse bietet nur statische Methoden. */
    private Benchmark() { }

    /**
     * Kennzahlen einer Messreihe, jeweils in Mikrosekunden pro Aufruf.
     *
     * @param min    kuerzester Aufruf
     * @param max    laengster Aufruf
     * @param mean   Mittelwert
     * @param median Median: die Haelfte der Aufrufe war schneller
     */
    private record Stats(double min, double max, double mean, double median) {
        /**
         * Berechnet die Kennzahlen aus den einzeln gemessenen Aufrufen.
         *
         * @param nanos Dauer jedes Aufrufs in Nanosekunden
         * @return Kennzahlen in Mikrosekunden
         */
        static Stats of(long[] nanos) {
            long[] s = nanos.clone();
            Arrays.sort(s);
            int n = s.length;
            double sum = 0;
            for (long v : s) sum += v;
            double median = n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2.0;
            return new Stats(s[0] / 1000.0, s[n - 1] / 1000.0, sum / n / 1000.0, median / 1000.0);
        }
    }

    /**
     * Prueft, ob eine Eingabe der Befehl {@code bench} ist, und liest die optionale Anzahl der Aufrufe.
     *
     * @param line Eingabezeile, darf {@code null} sein
     * @return Anzahl der Aufrufe je Messreihe ({@link #DEFAULT_CALLS} ohne Angabe) oder 0, wenn die Eingabe kein
     *         {@code bench}-Befehl ist
     */
    public static int parseCalls(String line) {
        if (line == null) return 0;
        Matcher m = COMMAND.matcher(line);
        if (!m.matches()) return 0;
        return m.group(1) == null ? DEFAULT_CALLS : Math.max(1, Integer.parseInt(m.group(1)));
    }

    /**
     * Fuehrt alle Messreihen aus und gibt Ergebnistabelle, Aufschluesselung und Faktor auf der Konsole aus.
     *
     * @param remote Stub zum {@link GameService} des Servers
     * @param peer   RPC-Endpunkt der Verbindung, fuer den leeren Aufruf {@link RpcPeer.Ping#ping()}
     * @param server Adresse des Servers, zur Erkennung des Szenarios
     * @param n      Anzahl gemessener Aufrufe je Messreihe
     * @throws RpcException wenn die Verbindung zum Server waehrend der Messung abbricht
     */
    public static void run(GameService remote, RpcPeer peer, InetAddress server, int n) {
        boolean sameHost = sameHost(server);
        String scenario = sameHost ? "localhost" : "Netzwerk";
        System.out.println();
        System.out.println("=== Benchmark getState(): lokal vs. remote ueber " + scenario
                + (sameHost ? " (Server auf demselben Rechner)" : " (Server " + server.getHostAddress() + ")") + " ===");
        System.out.println(n + " Aufrufe je Messreihe, jeder Aufruf einzeln gemessen, vorher Warm-up (JIT).");
        System.out.println("Hinweis: Server und Client ohne -Drpc.verbose=true starten, sonst wird die Konsolenausgabe mitgemessen.");
        if (Boolean.getBoolean("rpc.verbose"))
            System.out.println("WARNUNG: Dieser Client laeuft mit -Drpc.verbose=true, die Remote-Werte sind zu hoch!");

        Game local = localGame();
        GameState sample = remote.getState();   // echter Zustand vom Server fuer die Marshalling-Messung

        System.out.print("Messe:");
        Stats lokal = measure("lokal", WARMUP_LOCAL, n, local::getState)[0];
        Stats marshal = measure("Marshalling", WARMUP_MARSHAL, n, () -> RpcPeer.marshalRoundTrip(sample, GameState.class))[0];
        RpcPeer.Ping pinger = pingStub(peer);
        Stats rem;
        Stats ping = null;
        if (pinger != null) {
            Stats[] r = measure("remote+ping", WARMUP_REMOTE, n, remote::getState, pinger::ping);   // abwechselnd
            rem = r[0];
            ping = r[1];
        } else {
            rem = measure("remote", WARMUP_REMOTE, n, remote::getState)[0];
        }
        System.out.println(" - fertig");

        System.out.println();
        printTableHeader();
        printRow("Lokal: getState() im selben Prozess", lokal);
        printRow("Remote (" + scenario + "): getState() per RPC", rem);
        printRow("  Teil: leerer RPC-Aufruf (ping)", ping);
        printRow("  Teil: Marshalling GameState (Gson)", marshal);

        double total = rem.median();
        double transport = ping == null ? 0 : ping.median();
        double rest = total - transport - marshal.median() - lokal.median();
        System.out.println();
        System.out.println("Wofuer die Zeit beim Remote-Aufruf draufgeht (Median):");
        if (ping != null) printPart("Transport: TCP, Systemaufrufe, Threads, Netz (ping)", transport, total);
        else System.out.println("  Transport: nicht gemessen (Server kennt den Dienst \"rpc\" nicht)");
        printPart("Marshalling/Unmarshalling GameState (Gson)", marshal.median(), total);
        printPart("Spiellogik getState() (wie lokal)", lokal.median(), total);
        printPart("Rest (Reflection, Messschwankung)", rest, total);
        printPart("Remote-Aufruf gesamt", total, total);
        System.out.println();
        System.out.printf(Locale.GERMANY, "Ergebnis: Remote ist ca. %.0fx langsamer als lokal (Median).%n", total / lokal.median());
    }

    /**
     * Zeitmessung fuer die nicht verteilte Version: misst nur lokal, da es keinen Server gibt.
     *
     * <p>Gemessen werden {@link GameService#getState()} auf dem laufenden Spiel und zum Vergleich das Marshalling des
     * {@link GameState} ohne Netz. Das Marshalling ist der Teil eines Remote-Aufrufs, der sich auch lokal messen
     * laesst; die vollstaendigen Remote-Werte liefert {@code bench} im Client der verteilten Version.
     *
     * @param game das lokale Spiel
     * @param n    Anzahl gemessener Aufrufe je Messreihe
     */
    public static void runLocal(GameService game, int n) {
        System.out.println();
        System.out.println("=== Benchmark getState(): lokale Version (kein Netzwerk) ===");
        System.out.println(n + " Aufrufe je Messreihe, jeder Aufruf einzeln gemessen, vorher Warm-up (JIT).");
        GameState sample = game.getState();

        System.out.print("Messe:");
        Stats lokal = measure("lokal", WARMUP_LOCAL, n, game::getState)[0];
        Stats marshal = measure("Marshalling", WARMUP_MARSHAL, n, () -> RpcPeer.marshalRoundTrip(sample, GameState.class))[0];
        System.out.println(" - fertig");

        System.out.println();
        printTableHeader();
        printRow("Lokal: getState() im selben Prozess", lokal);
        printRow("Vergleich: Marshalling GameState (Gson)", marshal);
        System.out.println();
        System.out.printf(Locale.GERMANY, "Schon das Marshalling allein dauert ca. %.0fx so lange wie der lokale Aufruf (Median).%n",
                marshal.median() / lokal.median());
        System.out.println("Remote-Werte: Server (Benchmark) starten und im Client der verteilten Version 'bench' eingeben.");
    }

    /**
     * Fuehrt eine oder mehrere Messreihen aus: erst das Warm-up, dann {@code n} einzeln gemessene Aufrufe je Reihe.
     *
     * <p>Bei mehreren Aufrufen wird abwechselnd gemessen (Aufruf 1, Aufruf 2, Aufruf 1, ...). So laufen alle Reihen
     * unter denselben Bedingungen, statt dass z.B. die erste Reihe noch nicht optimierten Code trifft.
     *
     * @param name   Bezeichnung fuer die Fortschrittsanzeige
     * @param warmup Anzahl der Durchgaenge vor der Messung
     * @param n      Anzahl der gemessenen Aufrufe je Reihe
     * @param calls  zu messende Aufrufe, je einer pro Messreihe
     * @return Kennzahlen je Messreihe, in der Reihenfolge von {@code calls}
     */
    private static Stats[] measure(String name, int warmup, int n, Supplier<?>... calls) {
        System.out.print(" " + name);
        System.out.flush();
        for (int i = 0; i < warmup; i++) for (Supplier<?> c : calls) sink = c.get();
        long[][] t = new long[calls.length][n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < calls.length; k++) {
                long start = System.nanoTime();
                sink = calls[k].get();
                t[k][i] = System.nanoTime() - start;
            }
        }
        Stats[] result = new Stats[calls.length];
        for (int k = 0; k < calls.length; k++) result[k] = Stats.of(t[k]);
        return result;
    }

    /**
     * Erzeugt den Stub fuer den leeren Dienst {@code "rpc"} und prueft mit einem ersten Aufruf, ob der Server ihn kennt.
     *
     * @param peer RPC-Endpunkt der Verbindung
     * @return Stub fuer {@link RpcPeer.Ping} oder {@code null}, wenn der Server den Dienst nicht kennt (aeltere Version)
     */
    private static RpcPeer.Ping pingStub(RpcPeer peer) {
        RpcPeer.Ping ping = peer.proxy("rpc", RpcPeer.Ping.class);
        try {
            ping.ping();
            return ping;
        } catch (RpcException e) {
            System.out.print(" (ping nicht moeglich: " + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * Erzeugt ein lokales Spiel mit zwei Spielern, damit der lokale Zustand etwa so gross ist wie der des Servers.
     *
     * @return neues Spiel mit den Spielern Alice und Bob
     */
    private static Game localGame() {
        Game g = new Game();
        try {
            g.join(new Player("Alice"));
            g.join(new Player("Bob"));
        } catch (GameException e) {
            throw new IllegalStateException(e);   // kann bei einem neuen, leeren Spiel nicht passieren
        }
        return g;
    }

    /**
     * Prueft, ob der Server auf demselben Rechner laeuft (Loopback-Adresse oder eigene Netzwerkadresse).
     *
     * @param server Adresse des Servers
     * @return {@code true}, wenn Client und Server auf demselben Rechner laufen
     */
    private static boolean sameHost(InetAddress server) {
        try {
            return server.isLoopbackAddress() || NetworkInterface.getByInetAddress(server) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    /** Gibt die Kopfzeile der Ergebnistabelle aus: Minimum, Maximum, Mittelwert, Median. */
    private static void printTableHeader() {
        System.out.printf("%-42s %10s %10s %10s %10s%n", "[Mikrosekunden pro Aufruf]", "Minimum", "Maximum", "Mittelwert", "Median");
    }

    /**
     * Gibt eine Zeile der Ergebnistabelle aus.
     *
     * @param label Bezeichnung der Messreihe
     * @param s     Kennzahlen oder {@code null}, wenn nicht gemessen
     */
    private static void printRow(String label, Stats s) {
        if (s == null) System.out.printf("%-42s %10s%n", label, "nicht gemessen");
        else System.out.printf(Locale.GERMANY, ROW, label, s.min(), s.max(), s.mean(), s.median());
    }

    /**
     * Gibt eine Zeile der Aufschluesselung mit Anteil an der Gesamtzeit aus.
     *
     * @param label Bezeichnung des Anteils
     * @param us    Dauer in Mikrosekunden
     * @param total Gesamtdauer des Remote-Aufrufs in Mikrosekunden
     */
    private static void printPart(String label, double us, double total) {
        System.out.printf(Locale.GERMANY, PART, label, us, 100.0 * us / total);
    }
}
