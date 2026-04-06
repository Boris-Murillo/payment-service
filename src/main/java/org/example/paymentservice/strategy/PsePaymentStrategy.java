package org.example.paymentservice.strategy;

import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PsePaymentStrategy implements PaymentMethodStrategy {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PSE;
    }

    @Override
    public Map<String, Object> buildPaymentMethodData(OrderEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "PSE");
        data.put("user_type", event.getUserType());
        data.put("user_legal_id_type", event.getUserLegalIdType());
        data.put("user_legal_id", event.getUserLegalId());
        data.put("financial_institution_code", event.getFinancialInstitutionCode());
        return data;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}