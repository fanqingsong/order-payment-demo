package com.example.paymentservice.messaging;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order-created";
    public static final String ORDER_CREATED_DLT = "order-created.DLT";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";

    private KafkaTopics() {
    }
}
