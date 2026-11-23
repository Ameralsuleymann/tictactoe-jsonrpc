package tictactoe.rpc;

import com.google.gson.JsonElement;
import javax.jms.Destination;
import tictactoe.clock.VectorClock;

/**
 * Eine empfangene Nachricht, so wie das {@link Skeleton} sie braucht.
 *
 * @param method        Methodenname (Header {@code JMSType})
 * @param body          das eine Argument als JSON ({@code JsonNull} bei Methoden ohne Argument)
 * @param stamp         Vektoruhr-Zeitstempel des Absenders (Property {@code vectorClock})
 * @param replyTo       Antwortadresse des Absenders (Header {@code JMSReplyTo}), {@code null} bei Server-Nachrichten
 * @param correlationId Kennung der Anfrage (Header {@code JMSCorrelationID}), z.B. {@code Alice-3}
 */
public record Call(String method, JsonElement body, VectorClock stamp, Destination replyTo, String correlationId) { }
