package org.example.paymentservice.service;

import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.dto.wompi.WompiTransactionResponse;
import org.example.paymentservice.event.PaymentEventPublisher;
import org.example.paymentservice.model.Payment;
import org.example.paymentservice.model.PaymentMethod;
import org.example.paymentservice.model.PaymentStatus;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.strategy.CardPaymentStrategy;
import org.example.paymentservice.strategy.PaymentStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private WompiService wompiService;

    private PaymentStrategyFactory strategyFactory;

    private PaymentService paymentService;

    private UUID orderId;
    private UUID customerId;
    private OrderEvent orderEvent;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        customerId = UUID.randomUUID();

        strategyFactory = new PaymentStrategyFactory(List.of(new CardPaymentStrategy()));
        paymentService  = new PaymentService(paymentRepository, paymentEventPublisher, wompiService, strategyFactory);

        orderEvent = new OrderEvent();
        orderEvent.setEventType("ORDER_CREATED");
        orderEvent.setOrderId(orderId);
        orderEvent.setCustomerId(customerId);
        orderEvent.setAmount(new BigDecimal("59.98"));
        orderEvent.setPaymentMethod("CARD");
        orderEvent.setCustomerEmail("test@example.com");
        orderEvent.setCardToken("tok_test_123");
    }

    // --- processPayment ---

    @Test
    void processPayment_savesPaymentAndPublishesEventForCard() {
        WompiTransactionResponse.TransactionData wompiResult = new WompiTransactionResponse.TransactionData();
        wompiResult.setId("wompi-tx-001");
        wompiResult.setStatus("APPROVED");
        wompiResult.setReference(orderId.toString());

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(wompiService.charge(any(Payment.class), any(OrderEvent.class))).thenReturn(wompiResult);

        paymentService.processPayment(orderEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        verify(paymentEventPublisher).publishPaymentResult(any(Payment.class));

        Payment saved = captor.getAllValues().get(1);
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getAmount()).isEqualByComparingTo("59.98");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(saved.getWompiTransactionId()).isEqualTo("wompi-tx-001");
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void processPayment_skipsWhenPaymentAlreadyExists() {
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        paymentService.processPayment(orderEvent);

        verify(wompiService, never()).charge(any(), any());
        verify(paymentRepository, never()).save(any());
        verify(paymentEventPublisher, never()).publishPaymentResult(any());
    }

    @Test
    void processPayment_mapsDeclinedToFailed() {
        WompiTransactionResponse.TransactionData wompiResult = new WompiTransactionResponse.TransactionData();
        wompiResult.setId("wompi-tx-002");
        wompiResult.setStatus("DECLINED");
        wompiResult.setReference(orderId.toString());

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(wompiService.charge(any(Payment.class), any(OrderEvent.class))).thenReturn(wompiResult);

        paymentService.processPayment(orderEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // --- findByOrderId ---

    @Test
    void findByOrderId_returnsResponse_whenFound() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("59.98"))
                .status(PaymentStatus.APPROVED)
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.findByOrderId(orderId);

        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.getAmount()).isEqualByComparingTo("59.98");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void findByOrderId_throws_whenNotFound() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findByOrderId(orderId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }
}
