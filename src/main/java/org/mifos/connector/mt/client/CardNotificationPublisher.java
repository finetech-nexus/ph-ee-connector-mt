package org.mifos.connector.mt.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes the card operation on the notification topic. notification-api turns that event into the mobile notice.
 * When Kafka is not configured, the same payload is posted to notification-api directly.
 */
@Component
public class CardNotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(CardNotificationPublisher.class);
    private final MtBankClient bank;
    private final ObjectMapper json;
    private final String bootstrap;
    private final String topic;
    private volatile KafkaProducer<String, String> producer;

    public CardNotificationPublisher(MtBankClient bank, ObjectMapper json,
            @Value("${mt.kafka.bootstrap-servers:}") String bootstrap,
            @Value("${mt.kafka.payment-confirmation-topic:notification.payment-confirmation}") String topic) {
        this.bank = bank;
        this.json = json;
        this.bootstrap = bootstrap == null ? "" : bootstrap.trim();
        this.topic = topic;
    }

    public void publish(Map<String, Object> body, String action) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("tenantId", body.get("tenantId"));
        event.put("action", action);
        event.put("entity", "CLIENT");
        event.put("resourceId", body.get("userId"));
        event.put("notificationType", "PAYMENT_CONFIRMATION");
        event.put("payload", body);
        if (bootstrap.isBlank()) {
            bank.notifyCardholder(body);
            return;
        }
        try {
            String key = body.get("clientReference") == null ? String.valueOf(body.get("transactionId")) : String.valueOf(body.get("clientReference"));
            producer().send(new ProducerRecord<>(topic, key, json.writeValueAsString(event))).get();
        } catch (Exception ex) {
            log.warn("Kafka notification was not published, falling back to notification-api: {}", ex.getMessage());
            bank.notifyCardholder(body);
        }
    }

    private KafkaProducer<String, String> producer() {
        KafkaProducer<String, String> current = producer;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (producer == null) {
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.ACKS_CONFIG, "1");
                props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "5000");
                producer = new KafkaProducer<>(props);
            }
            return producer;
        }
    }
}
