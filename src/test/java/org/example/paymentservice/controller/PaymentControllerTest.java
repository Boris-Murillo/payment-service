package org.example.paymentservice.controller;

import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.model.PaymentStatus;
import org.example.paymentservice.service.PaymentNotFoundException;
import org.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void findByOrderId_shouldReturn200_whenFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("59.98"))
                .status(PaymentStatus.APPROVED)
                .build();

        when(paymentService.findByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(59.98));
    }

    @Test
    void findByOrderId_shouldReturn404_whenNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        String errorMessage = "Payment not found for order: " + orderId;

        when(paymentService.findByOrderId(orderId))
                .thenThrow(new PaymentNotFoundException(errorMessage));

        mockMvc.perform(get("/api/payments/order/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(errorMessage));
    }
}
