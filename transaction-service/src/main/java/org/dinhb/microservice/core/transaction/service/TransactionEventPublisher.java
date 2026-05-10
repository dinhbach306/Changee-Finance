package org.dinhb.microservice.core.transaction.service;

import org.dinhb.microservice.core.common.event.KafkaTopics;
import org.dinhb.microservice.core.common.event.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafka;

    public TransactionEventPublisher(KafkaTemplate<String, TransactionEvent> kafka) {
        this.kafka = kafka;
    }

    public void publish(TransactionEvent event) {
        String topic = switch (event.status()) {
            case CREATED -> KafkaTopics.TRANSACTION_CREATED;
            case COMPLETED -> KafkaTopics.TRANSACTION_COMPLETED;
            case FAILED -> KafkaTopics.TRANSACTION_FAILED;
        };
        kafka.send(topic, event.transactionId().toString(), event);
    }
}
