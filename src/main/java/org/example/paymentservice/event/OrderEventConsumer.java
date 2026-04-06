package org.example.paymentservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.config.RabbitMQConfig;
import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.service.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_ORDER_QUEUE)
    public void onOrderCreated(OrderEvent event) {
        log.info("Received ORDER_CREATED event for order {}", event.getOrderId());
        paymentService.processPayment(event);
    }
}
