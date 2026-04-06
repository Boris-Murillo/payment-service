package org.example.paymentservice.strategy;

import org.example.paymentservice.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentMethod, PaymentMethodStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentMethodStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PaymentMethodStrategy::getPaymentMethod, Function.identity()));
    }

    public PaymentMethodStrategy getStrategy(PaymentMethod method) {
        PaymentMethodStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("No payment strategy found for method: " + method);
        }
        return strategy;
    }
}