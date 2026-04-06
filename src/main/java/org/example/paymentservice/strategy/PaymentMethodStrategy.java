package org.example.paymentservice.strategy;

import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.model.PaymentMethod;

import java.util.Map;

public interface PaymentMethodStrategy {

    PaymentMethod getPaymentMethod();

    /**
     * Builds the {@code payment_method} object sent to the Wompi API.
     */
    Map<String, Object> buildPaymentMethodData(OrderEvent event);

    /**
     * Whether the payment result is delivered asynchronously via webhook.
     * CARD is synchronous. PSE, Nequi, and Daviplata are asynchronous.
     */
    boolean isAsync();
}