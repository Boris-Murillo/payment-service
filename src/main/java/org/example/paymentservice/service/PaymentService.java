package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.dto.wompi.WompiTransactionResponse;
import org.example.paymentservice.dto.wompi.WompiWebhookEvent;
import org.example.paymentservice.event.PaymentEventPublisher;
import org.example.paymentservice.model.Payment;
import org.example.paymentservice.model.PaymentMethod;
import org.example.paymentservice.model.PaymentStatus;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.strategy.PaymentMethodStrategy;
import org.example.paymentservice.strategy.PaymentStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final WompiService wompiService;
    private final PaymentStrategyFactory strategyFactory;

    @Transactional
    public void processPayment(OrderEvent event) {
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Payment already exists for order {}, skipping", event.getOrderId());
            return;
        }

        PaymentMethod method = PaymentMethod.valueOf(event.getPaymentMethod());

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getAmount())
                .paymentMethod(method)
                .build();

        paymentRepository.save(payment);

        WompiTransactionResponse.TransactionData result = wompiService.charge(payment, event);

        payment.setWompiTransactionId(result.getId());
        payment.setWompiReference(result.getReference());
        payment.setRedirectUrl(result.getRedirectUrl());
        payment.setStatus(mapStatus(result.getStatus()));

        paymentRepository.save(payment);

        // For synchronous methods (CARD), publish the result immediately.
        // For async methods (PSE, Nequi, Daviplata), the webhook will publish it.
        PaymentMethodStrategy strategy = strategyFactory.getStrategy(method);
        if (!strategy.isAsync()) {
            paymentEventPublisher.publishPaymentResult(payment);
        }

        log.info("Payment {} for order {} via {}", payment.getStatus(), event.getOrderId(), method);
    }

    @Transactional
    public void handleWompiWebhook(WompiWebhookEvent event) {
        WompiWebhookEvent.TransactionData tx = event.getData().getTransaction();

        Payment payment = paymentRepository.findByWompiReference(tx.getReference())
                .orElseGet(() -> paymentRepository.findByWompiTransactionId(tx.getId())
                        .orElseThrow(() -> new PaymentNotFoundException(
                                "Payment not found for Wompi reference: " + tx.getReference())));

        PaymentStatus newStatus = mapStatus(tx.getStatus());

        if (payment.getStatus() == newStatus) {
            log.info("Webhook: payment {} already in status {}, skipping", payment.getId(), newStatus);
            return;
        }

        payment.setStatus(newStatus);
        paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentResult(payment);
        log.info("Webhook updated payment {} to {} for order {}", payment.getId(), newStatus, payment.getOrderId());
    }

    public PaymentResponse findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
    }

    private PaymentStatus mapStatus(String wompiStatus) {
        return switch (wompiStatus) {
            case "APPROVED" -> PaymentStatus.APPROVED;
            case "DECLINED", "ERROR", "VOIDED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}
