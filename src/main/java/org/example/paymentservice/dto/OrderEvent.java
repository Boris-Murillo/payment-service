package org.example.paymentservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderEvent {

    private String eventType;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String currency = "COP";

    /** Payment method type: CARD, PSE, NEQUI, DAVIPLATA */
    private String paymentMethod;

    private String customerEmail;

    // --- CARD fields ---
    /** Token obtained from Wompi.js tokenization on the frontend */
    private String cardToken;
    private Integer installments = 1;

    // --- PSE fields ---
    private String financialInstitutionCode;
    /** 0 = Natural person, 1 = Legal entity */
    private Integer userType;
    /** CC, NIT, CE, etc. */
    private String userLegalIdType;
    private String userLegalId;

    // --- NEQUI & DAVIPLATA fields ---
    private String phoneNumber;

    /** Required for async methods (PSE, Nequi, Daviplata): URL to redirect after payment */
    private String redirectUrl;
}