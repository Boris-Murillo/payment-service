package org.example.paymentservice.strategy;

import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DaviplataPaymentStrategy implements PaymentMethodStrategy {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.DAVIPLATA;
    }

    @Override
    public Map<String, Object> buildPaymentMethodData(OrderEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "DAVIPLATA");
        data.put("phone_number", event.getPhoneNumber());
        return data;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}