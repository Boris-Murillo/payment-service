package org.example.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.wompi.WompiWebhookEvent;
import org.example.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class WompiWebhookController {

    private final PaymentService paymentService;

    @Value("${wompi.integrity-key}")
    private String integrityKey;

    @PostMapping("/wompi")
    public ResponseEntity<Void> handleWompiEvent(@RequestBody WompiWebhookEvent event) {
        if (!isValidSignature(event)) {
            log.warn("Invalid Wompi webhook signature for event {}", event.getEvent());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Wompi webhook received: event={} status={} reference={}",
                event.getEvent(),
                event.getData().getTransaction().getStatus(),
                event.getData().getTransaction().getReference());

        paymentService.handleWompiWebhook(event);
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the webhook checksum.
     * Wompi computes: SHA256(property_values + timestamp + integrity_key)
     * where property_values are the values of signature.properties fields
     * in the order they appear, extracted from the transaction object.
     */
    private boolean isValidSignature(WompiWebhookEvent event) {
        try {
            WompiWebhookEvent.TransactionData tx = event.getData().getTransaction();
            WompiWebhookEvent.Signature sig = event.getSignature();

            if (sig == null || sig.getChecksum() == null || sig.getProperties() == null) {
                return false;
            }

            StringBuilder message = new StringBuilder();
            for (String property : sig.getProperties()) {
                String value = extractTransactionProperty(tx, property);
                if (value != null) {
                    message.append(value);
                }
            }
            message.append(event.getTimestamp());
            message.append(integrityKey);

            String computed = sha256Hex(message.toString());
            return computed.equals(sig.getChecksum());

        } catch (Exception e) {
            log.error("Error verifying Wompi webhook signature", e);
            return false;
        }
    }

    private String extractTransactionProperty(WompiWebhookEvent.TransactionData tx, String property) {
        return switch (property) {
            case "transaction.id" -> tx.getId();
            case "transaction.status" -> tx.getStatus();
            case "transaction.amount_in_cents" ->
                    tx.getAmountInCents() != null ? tx.getAmountInCents().toString() : null;
            case "transaction.currency" -> tx.getCurrency();
            case "transaction.payment_method_type" -> tx.getPaymentMethodType();
            default -> null;
        };
    }

    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}