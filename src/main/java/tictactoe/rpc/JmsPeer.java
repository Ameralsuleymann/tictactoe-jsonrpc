package tictactoe.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import tictactoe.clock.VectorClock;

/**
 * Transport über den Message-Broker ActiveMQ (JMS); ersetzt die TCP-Verbindung aus Praktikum 1.
 *
 * <p>Kanäle:
 * <ul>
 *   <li>{@value #REQUESTS} (Queue): alle Aufrufe Client &rarr; Server,</li>
 *   <li>{@value #STATE} (Topic): der neue Spielzustand an alle Clients (Publish/Subscribe = Observer),</li>
 *   <li>eine temporäre Queue je Client: Rückmeldungen an genau diesen Client.</li>
 * </ul>
 * Jede Nachricht ist eine {@link TextMessage}: Body = DTO als JSON, Header {@code JMSType} = Methodenname, Property
 * {@value #CLOCK} = Vektoruhr des Absenders, Property {@value #SENDER} = Name des Absenders. Anfragen tragen zusätzlich
 * {@code JMSReplyTo} und {@code JMSCorrelationID}. Gesendet wird ohne zu warten (fire-and-forget).
 *
 * <p>Vektoruhr: Vor jedem Senden {@link VectorClock#tickForSend()}, nach jedem Empfangen
 * {@link VectorClock#receive(VectorClock)}; beides wird mit Uhr vorher/nachher ausgegeben.
 */
public class JmsPeer implements AutoCloseable {
    /** Queue für alle Aufrufe Client &rarr; Server. */
    public static final String REQUESTS = "tictactoe.requests";
    /** Topic für den Spielzustand Server &rarr; alle Clients. */
    public static final String STATE = "tictactoe.state";
    /** Property mit der Vektoruhr des Absenders als JSON. */
    public static final String CLOCK = "vectorClock";
    /** Property mit dem Namen des Absenders. */
    public static final String SENDER = "sender";
    /** ActiveMQ-Property: Der Broker stellt die Nachricht erst nach so vielen Millisekunden zu. */
    public static final String DELAY = "AMQ_SCHEDULED_DELAY";
    /** Marshalling Java &harr; JSON für Stubs und Skeletons. */
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final String name;
    private final VectorClock clock;
    private final Connection connection;
    /** Session für alle Empfänger; ihre Nachrichten kommen nacheinander in einem Listener-Thread an. */
    private final Session receiveSession;
    /** Eigene Session zum Senden, weil JMS-Sessions nicht threadsicher sind. */
    private final Session sendSession;
    private final MessageProducer producer;

    /**
     * Verbindet sich mit dem Broker {@code tcp://<host>:61616}.
     *
     * @param brokerHost Rechner, auf dem der Broker läuft
     * @param name       eigener Prozessname, zugleich Name in der Vektoruhr
     * @throws JMSException wenn der Broker nicht erreichbar ist
     */
    public JmsPeer(String brokerHost, String name) throws JMSException {
        this.name = name;
        this.clock = new VectorClock(name);
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://" + brokerHost + ":61616");
        factory.setAlwaysSyncSend(true);   // send() kehrt erst zurück, wenn der Broker die Nachricht hat
        connection = factory.createConnection();
        receiveSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        sendSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = sendSession.createProducer(null);   // Ziel wird bei jedem send() angegeben
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        connection.start();
    }

    /** @return eigener Prozessname */
    public String name() { return name; }

    /** @return eigene Vektoruhr */
    public VectorClock clock() { return clock; }

    /**
     * @param queueName Name der Queue
     * @return die Queue
     * @throws JMSException bei Fehlern des Brokers
     */
    public Queue queue(String queueName) throws JMSException { return receiveSession.createQueue(queueName); }

    /**
     * @param topicName Name des Topics
     * @return das Topic
     * @throws JMSException bei Fehlern des Brokers
     */
    public Topic topic(String topicName) throws JMSException { return receiveSession.createTopic(topicName); }

    /**
     * Erzeugt eine temporäre Queue, die nur so lange existiert wie diese Verbindung (Antwortadresse eines Clients).
     *
     * @return die temporäre Queue
     * @throws JMSException bei Fehlern des Brokers
     */
    public TemporaryQueue temporaryQueue() throws JMSException { return receiveSession.createTemporaryQueue(); }

    /**
     * Empfängt alle Nachrichten einer Queue bzw. eines Topics und gibt sie an das Skeleton.
     *
     * @param from     Queue oder Topic
     * @param skeleton führt die Nachrichten aus
     * @throws JMSException bei Fehlern des Brokers
     */
    public void listen(Destination from, Skeleton skeleton) throws JMSException {
        receiveSession.createConsumer(from).setMessageListener(message -> receive(message, skeleton));
    }

    /**
     * Sendet eine Nachricht (fire-and-forget) und versieht sie mit dem Zeitstempel der eigenen Vektoruhr.
     *
     * @param to            Queue oder Topic
     * @param method        Methodenname
     * @param body          das eine Argument (DTO) oder {@code null}
     * @param replyTo       Antwortadresse oder {@code null}
     * @param correlationId Kennung der Anfrage oder {@code null}
     * @param delayMs       Verzögerung durch den Broker in ms, 0 = sofort
     * @throws JMSException bei Fehlern des Brokers
     */
    public synchronized void send(Destination to, String method, Object body, Destination replyTo,
                                  String correlationId, long delayMs) throws JMSException {
        TextMessage message = sendSession.createTextMessage(GSON.toJson(body));
        message.setJMSType(method);
        message.setStringProperty(SENDER, name);
        if (replyTo != null) message.setJMSReplyTo(replyTo);
        if (correlationId != null) message.setJMSCorrelationID(correlationId);
        if (delayMs > 0) message.setLongProperty(DELAY, delayMs);
        String before;
        VectorClock stamp;
        synchronized (clock) {             // Uhr vorher/nachher ohne Störung durch den Listener-Thread
            before = clock.toString();
            stamp = clock.tickForSend();
        }
        message.setStringProperty(CLOCK, stamp.toJson());
        producer.send(to, message);
        System.out.println("[VC " + name + "] sende " + method + describe(correlationId) + " an " + label(to)
                + (delayMs > 0 ? " (Broker verzögert " + delayMs + " ms)" : "") + "  Uhr " + before + " -> " + stamp);
    }

    /**
     * Schließt die Verbindung zum Broker; die temporäre Queue verschwindet dabei.
     *
     * @throws JMSException bei Fehlern des Brokers
     */
    @Override public void close() throws JMSException { connection.close(); }

    /** Listener-Thread: Vektoruhr anpassen, Nachricht auspacken und an das Skeleton geben. */
    private void receive(Message message, Skeleton skeleton) {
        try {
            TextMessage text = (TextMessage) message;
            VectorClock stamp = VectorClock.fromJson(text.getStringProperty(CLOCK));
            String before;
            String after;
            synchronized (clock) {
                before = clock.toString();
                clock.receive(stamp);
                after = clock.toString();
            }
            System.out.println("[VC " + name + "] empfange " + text.getJMSType() + describe(text.getJMSCorrelationID())
                    + " von " + text.getStringProperty(SENDER) + " mit " + stamp + "  Uhr " + before + " -> " + after);
            skeleton.invoke(new Call(text.getJMSType(), JsonParser.parseString(text.getText()), stamp,
                    text.getJMSReplyTo(), text.getJMSCorrelationID()));
        } catch (JMSException | RuntimeException e) {
            System.out.println("[JMS] Nachricht nicht verarbeitet: " + e);
        }
    }

    private static String describe(String correlationId) { return correlationId == null ? "" : " (" + correlationId + ")"; }

    private static String label(Destination d) { return d instanceof TemporaryQueue ? "Antwort-Queue" : d.toString(); }
}
