# Tic-Tac-Toe mit JSON-RPC (AI-VSP Praktikum 1, WiSe 2026/27)

Java 25 (LTS), Maven, Gson. Das Projekt entsteht in zwei Phasen (siehe Git-Tags):

| Tag | Inhalt |
|---|---|
| `phase1-lokal` | Nicht verteilte Anwendung: alle Fachklassen, ein Prozess, kein Netzwerk |
| `phase2-verteilt` | Client-Server ueber JSON-RPC 2.0 / TCP, Thin Client, Callbacks Server -> Client |

---

## Phase 1: lokale Anwendung

Start in IntelliJ: Run-Konfiguration **Lokal (Phase 1)** oder `tictactoe.local.LocalMain`.
Zwei Spieler (Alice = X, Bob = O) spielen abwechselnd an einer Konsole. Eingabe: `zeile spalte`, z. B. `1 1` oder `11`.
`neu` startet die Runde neu bzw. nach Spielende eine neue Runde, `q` beendet.

| Paket | Klasse | Rolle |
|---|---|---|
| `model` | `Player`, `Move`, `GameState`, `Board`, `Symbol`, `Status` | Fachobjekte (in Phase 2 die DTOs) |
| `api` | `GameService` | Schnittstelle zum Spiel (in Phase 2 Contract Client -> Server) |
| `api` | `GameListener` | Observer-Schnittstelle (in Phase 2 Contract Server -> Client) |
| `api` | `GameException` | Fachlicher Fehler (ungueltiger Zug, nicht am Zug, ...) |
| `game` | `Game` | Spiellogik, Regeln, Gewinnpruefung, Observer-Subject |
| `ui` | `ConsoleListener` | Konsolenausgabe des Spielfelds (Observer) |
| `local` | `LocalMain` | Startklasse, liest Befehle von der Konsole |

Design Patterns: **Observer** (`GameListener`), **Command** (`Move`), **Facade** (`GameService`),
in Phase 2 zusaetzlich **Proxy** (Stubs).

---

## Phase 2: verteilte Anwendung

### Starten
In IntelliJ: zuerst **Server (Phase 2)**, dann **Client Alice (Phase 2)** und **Client Bob (Phase 2)**.

Auf zwei Rechnern (z. B. ueber Hotspot oder Hamachi):
1. Server-Rechner: Port 5000 in der Windows-Firewall erlauben, **Server** starten.
   Der Server zeigt beim Start seine IPv4-Adressen an (bei Hamachi `25.x.x.x`).
2. Client-Rechner: **Client** starten und bei `Server-IP [Enter = localhost]:` die IP des Servers eingeben
   (optional `ip:port`). Bei einem Fehler wird mit Grund (abgelehnt, keine Antwort, Adresse unbekannt) erneut gefragt.

Ohne IntelliJ: `mvn package`, dann
```
java -Drpc.verbose=true -cp target/tictactoe-jsonrpc-1.0-jar-with-dependencies.jar tictactoe.server.GameServer 5000
java -cp target/tictactoe-jsonrpc-1.0-jar-with-dependencies.jar tictactoe.client.GameClient [host] [port] [name]
```
`host` ist nur die Vorgabe fuer die IP-Abfrage; ohne `name` fragt der Client nach dem Namen.

Client-Befehle: `zeile spalte` (oder `11`), `neu` (auch `neustart`, `weiter`, `reset`), `bench`, `q`.

Der Server laeuft dauerhaft: Nach Spielende geht es mit `neu` weiter (Rundenzaehler im `GameState`).
Trennt sich ein Client (`q` oder Verbindungsabbruch), meldet der Server den Spieler ab (`Game.leave`),
die Runde endet und ein neuer Client kann den freien Platz uebernehmen. Doppelte Namen werden abgelehnt.

### Neue Klassen
| Paket | Klasse | Rolle |
|---|---|---|
| `rpc` | `RpcPeer` | JSON-RPC 2.0 ueber TCP, Stub-Erzeugung (`proxy`), Skeleton/Dispatcher (`handleRequest`) |
| `rpc` | `RpcException` | Technische Fehler (Verbindung, Timeout, unbekannte Methode) |
| `server` | `GameServer`, `GameSession` | Socket-Server; pro Client eine Session, alle teilen ein `Game` |
| `client` | `GameClient` | Thin Client: Eingabe + Anzeige, keine Spiellogik |

---

## Liefergegenstaende

### 1. Nicht verteilte Anwendung
Tag `phase1-lokal`, Startklasse `LocalMain`. Sie enthaelt bereits alle Fachklassen der verteilten Version.

### 2. Designentscheidung C-S-Contract
```java
interface GameService  { Player join(Player); GameState makeMove(Move) throws GameException; GameState getState(); void reset(); }
interface GameListener { void onPlayerJoined(Player); void onPlayerLeft(Player); void onStateChanged(GameState); }   // Server -> Client
```
- **Bidirektional:** Client ruft `game.*`, Server ruft `listener.*` ueber denselben TCP-Socket zurueck.
- **DTOs:** Client -> Server `Player`, `Move` (enthaelt `Player`); Server -> Client `Player`, `GameState` (enthaelt `Board`, `Player`).
- **Thin Client:** Regeln, Zugreihenfolge und Gewinnpruefung liegen nur im Server, der Client zeigt an.
- **Alternativen:**
  - *Fat Client:* Client prueft Zuege vorab (spart Roundtrips), aber Regeln doppelt und Manipulation moeglich.
  - *Polling* statt Callback: einfacher, aber Verzoegerung und unnoetige Last.
  - *Delta-Updates* statt vollem `GameState`: weniger Daten, aber komplexer und fehleranfaellig.
  - *Java RMI:* weniger Code, aber binaeres Format, nicht menschenlesbar. *gRPC:* IDL + Protobuf, ebenfalls binaer.
- Eigene schlanke JSON-RPC-Schicht statt fertiger Bibliothek, damit Stub, Marshalling und Dispatch sichtbar sind. Gson uebernimmt das Marshalling.

Beispielnachricht (mit `-Drpc.verbose=true` sichtbar):
```
{"jsonrpc":"2.0","method":"game.makeMove","params":[{"player":{"name":"Alice","symbol":"X"},"row":1,"col":1}],"id":3}
```

### 3. Verteilung auf zwei Rechner
Siehe "Starten" oben.

### 4. Klassen mit Stubs
| Seite | Stub fuer | Implementierung auf der Gegenseite |
|---|---|---|
| Client | `GameService` (`peer.proxy("game", ...)`) | Server: `GameSession` -> `Game` |
| Server | `GameListener` (`peer.proxy("listener", ...)`) | Client: `ConsoleListener` |

`RpcPeer.handleRequest` ist auf beiden Seiten der generische Skeleton (Reflection).
DTOs brauchen keine Stubs, sie werden per Wert (als Kopie) uebertragen.

### 5. Sequenzdiagramm `makeMove`
```mermaid
sequenceDiagram
    participant UI as GameClient (Eingabe)
    participant Stub as GameService-Stub (Client)
    participant CP as RpcPeer Client
    participant SP as RpcPeer Server (Skeleton)
    participant S as GameSession
    participant G as Game
    participant LS as GameListener-Stub (Server)
    participant CL as ConsoleListener (Client)
    UI->>Stub: makeMove(Move)
    Stub->>Stub: Marshalling (Gson): Move -> JSON
    Stub->>CP: Request {"method":"game.makeMove","params":[...],"id":7}
    CP->>SP: TCP, eine JSON-Zeile
    SP->>SP: Unmarshalling JSON -> Move, Methode per Reflection suchen
    SP->>S: makeMove(Move)
    S->>G: makeMove(Move)
    G->>G: Regeln pruefen, Board aendern, Gewinn pruefen
    G->>LS: onStateChanged(GameState)
    LS->>CL: Notification (ohne id) ueber TCP
    CL->>CL: Unmarshalling, Spielfeld ausgeben
    G-->>S: GameState
    S-->>SP: GameState
    SP->>SP: Marshalling GameState -> JSON
    SP-->>CP: Response {"id":7,"result":{...}}
    CP-->>Stub: Future zu id 7 wird erfuellt
    Stub->>Stub: Unmarshalling JSON -> GameState
    Stub-->>UI: GameState
    Note over SP,CP: Fehlerfall: {"id":7,"error":{"message":"...","data":{"exception":"tictactoe.api.GameException"}}}
```

### 6. Zeitvergleich lokal vs. remote
Im Client `bench` eingeben: je 2000x `getState()` lokal (`Game` im selben Prozess) und remote, nach JIT-Warm-up.
Testlauf auf einem Rechner (Loopback): lokal ca. 1,7 us, remote ca. 380 us pro Aufruf (Faktor ca. 200).
Ueber WLAN/Hotspot wird remote deutlich langsamer, dort neu messen.
Ursachen: Marshalling, Systemaufrufe, TCP-Roundtrip, Thread-Wechsel, Netzlatenz.

### 7. Uebertragung einer Exception
Belegtes Feld waehlen, ausserhalb des eigenen Zuges setzen oder `5 5` eingeben:
Server wirft `GameException` -> JSON-`error` mit Klassenname -> Client-Stub erzeugt `GameException` neu
(weil in der Signatur deklariert) -> Client faengt sie und zeigt "Server-Fehler (GameException): ...".
Nicht deklarierte Fehler, Verbindungsabbruch und Timeout (10 s) erscheinen als `RpcException`.

### 8. Transparenz
| Art | Erreicht? | Begruendung |
|---|---|---|
| Zugriffstransparenz | Ja | Aufruf ueber `GameService`, lokal `Game`, remote Stub; gleicher Aufrufcode (`bench` nutzt beides) |
| Ortstransparenz | Teilweise | Host/Port nur beim Verbindungsaufbau, danach unsichtbar; kein Namensdienst |
| Fehlertransparenz | Nein | Netzfehler werden als `RpcException` sichtbar, kein automatischer Reconnect (der Server gibt den Platz aber frei, ein Client kann neu beitreten) |
| Nebenlaeufigkeitstransparenz | Teilweise | `Game` ist `synchronized`, Callbacks werden serialisiert abgearbeitet |
| Leistungstransparenz | Nein | Remote-Aufrufe sind um Groessenordnungen langsamer |
| Migrations-/Replikationstransparenz | Nein | Ein Server, keine Replikation |
| Hinweis | | Parameter werden per Wert kopiert (DTOs), nicht per Referenz wie lokal |
