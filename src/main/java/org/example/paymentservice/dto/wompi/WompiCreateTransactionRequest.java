package org.example.paymentservice.dto.wompi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WompiCreateTransactionRequest {

    @JsonProperty("amount_in_cents")
    private Long amountInCents;

    private String currency;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("payment_method")
    private Map<String, Object> paymentMethod;

    private String reference;

    @JsonProperty("acceptance_token")
    private String acceptanceToken;

    @JsonProperty("acceptance_personal_auth")
    private String acceptancePersonalAuth;

    @JsonProperty("signature")
    private String integritySignature;

    /** Required for async methods: PSE, Nequi, Daviplata */
    @JsonProperty("redirect_url")
    private String redirectUrl;
}