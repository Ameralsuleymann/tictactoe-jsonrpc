# Tic-Tac-Toe mit JSON-RPC (AI-VSP Praktikum 1, WiSe 2026/27)

Java 25 (LTS), Maven. Das Projekt entsteht in zwei Phasen (siehe Git-Tags):

| Tag | Inhalt |
|---|---|
| `phase1-lokal` | Nicht verteilte Anwendung: alle Fachklassen, ein Prozess, kein Netzwerk |
| `phase2-verteilt` | Client-Server ueber JSON-RPC 2.0 / TCP, Thin Client, Callbacks Server -> Client |

## Phase 1: lokale Anwendung

Start in IntelliJ: Run-Konfiguration **Lokal (Phase 1)** oder `tictactoe.local.LocalMain`.
Zwei Spieler (Alice = X, Bob = O) spielen abwechselnd an einer Konsole. Eingabe: `zeile spalte`, z. B. `1 1`.

### Klassen
| Paket | Klasse | Rolle |
|---|---|---|
| `model` | `Player`, `Move`, `GameState`, `Board`, `Symbol`, `Status` | Fachobjekte (in Phase 2 die DTOs) |
| `api` | `GameService` | Schnittstelle zum Spiel (in Phase 2 Contract Client -> Server) |
| `api` | `GameListener` | Observer-Schnittstelle (in Phase 2 Contract Server -> Client) |
| `api` | `GameException` | Fachlicher Fehler (ungueltiger Zug, nicht am Zug, ...) |
| `game` | `Game` | Spiellogik, Regeln, Gewinnpruefung, Observer-Subject |
| `ui` | `ConsoleListener` | Konsolenausgabe des Spielfelds (Observer) |
| `local` | `LocalMain` | Startklasse, liest Befehle von der Konsole |

### Design Patterns
- **Observer:** `Game` benachrichtigt alle `GameListener` bei jeder Zustandsaenderung.
- **Command:** `Move` kapselt einen Spielbefehl als Objekt.
- **Facade:** `GameService` ist die einzige Schnittstelle nach aussen auf die Spiellogik.

Die Schnittstellen sind schon so geschnitten, dass sie in Phase 2 unveraendert ueber das Netz gehen koennen.
