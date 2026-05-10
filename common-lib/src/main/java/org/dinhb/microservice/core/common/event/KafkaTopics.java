package org.dinhb.microservice.core.common.event;

public final class KafkaTopics {

    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String TRANSACTION_COMPLETED = "transaction.completed";
    public static final String TRANSACTION_FAILED = "transaction.failed";

    private KafkaTopics() {}
}
