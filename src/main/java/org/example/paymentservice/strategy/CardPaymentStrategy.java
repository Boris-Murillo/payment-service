package org.example.paymentservice.strategy;

import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CardPaymentStrategy implements PaymentMethodStrategy {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public Map<String, Object> buildPaymentMethodData(OrderEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "CARD");
        data.put("token", event.getCardToken());
        data.put("installments", event.getInstallments() != null ? event.getInstallments() : 1);
        return data;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}