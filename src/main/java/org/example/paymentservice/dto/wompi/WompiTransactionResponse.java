package org.example.paymentservice.dto.wompi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WompiTransactionResponse {

    private TransactionData data;

    @Data
    public static class TransactionData {
        private String id;
        /** APPROVED, PENDING, DECLINED, ERROR, VOIDED */
        private String status;
        private String reference;
        @JsonProperty("payment_method_type")
        private String paymentMethodType;
        /** Present for PSE — URL to redirect the user to their bank */
        @JsonProperty("redirect_url")
        private String redirectUrl;
    }
}