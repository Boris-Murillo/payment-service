package org.example.paymentservice.dto.wompi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WompiWebhookEvent {

    /** e.g. "transaction.updated" */
    private String event;

    private EventData data;

    private Long timestamp;

    private String environment;

    private Signature signature;

    @Data
    public static class EventData {
        private TransactionData transaction;
    }

    @Data
    public static class TransactionData {
        private String id;
        /** APPROVED, PENDING, DECLINED, ERROR, VOIDED */
        private String status;
        private String reference;
        @JsonProperty("amount_in_cents")
        private Long amountInCents;
        private String currency;
        @JsonProperty("payment_method_type")
        private String paymentMethodType;
    }

    @Data
    public static class Signature {
        private String checksum;
        /** Property paths used to compute the checksum, e.g. ["transaction.id", "transaction.status", ...] */
        private List<String> properties;
    }
}