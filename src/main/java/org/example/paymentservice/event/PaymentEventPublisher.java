package org.example.paymentservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.config.RabbitMQConfig;
import org.example.paymentservice.model.Payment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentResult(Payment payment) {
        String routingKey = switch (payment.getStatus()) {
            case APPROVED -> "payment.approved";
            case FAILED   -> "payment.failed";
            default       -> "payment.unknown";
        };

        Map<String, Object> event = Map.of(
                "eventType", "PAYMENT_" + payment.getStatus().name(),
                "orderId", payment.getOrderId().toString(),
                "status", payment.getStatus().name()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, routingKey, event);
        log.info("Published {} event for order {}", routingKey, payment.getOrderId());
    }
}
