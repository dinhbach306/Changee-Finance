package org.dinhb.microservice.core.notification.service;

import org.dinhb.microservice.core.common.event.KafkaTopics;
import org.dinhb.microservice.core.common.event.TransactionEvent;
import org.dinhb.microservice.core.notification.domain.Notification;
import org.dinhb.microservice.core.notification.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final NotificationRepository repository;

    public TransactionEventConsumer(NotificationRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = {
            KafkaTopics.TRANSACTION_CREATED,
            KafkaTopics.TRANSACTION_COMPLETED,
            KafkaTopics.TRANSACTION_FAILED
    }, groupId = "${spring.kafka.consumer.group-id}")
    public void handle(TransactionEvent event) {
        String message = formatMessage(event);
        log.info("Notification: {}", message);
        repository.save(new Notification(event.initiatorUserId(), "TRANSACTION_" + event.status(), message));
    }

    private String formatMessage(TransactionEvent e) {
        return switch (e.status()) {
            case CREATED -> "Transaction %s created: %s %s from %s to %s".formatted(
                    e.transactionId(), e.amount(), e.currency(), e.fromAccountId(), e.toAccountId());
            case COMPLETED -> "Transaction %s completed successfully (%s %s)".formatted(
                    e.transactionId(), e.amount(), e.currency());
            case FAILED -> "Transaction %s failed: %s".formatted(e.transactionId(), e.reason());
        };
    }
}
