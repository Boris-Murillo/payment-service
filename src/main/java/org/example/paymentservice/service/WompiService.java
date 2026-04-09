package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.OrderEvent;
import org.example.paymentservice.dto.wompi.WompiCreateTransactionRequest;
import org.example.paymentservice.dto.wompi.WompiMerchantResponse;
import org.example.paymentservice.dto.wompi.WompiTransactionResponse;
import org.example.paymentservice.model.Payment;
import org.example.paymentservice.strategy.PaymentMethodStrategy;
import org.example.paymentservice.strategy.PaymentStrategyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WompiService {

    private final RestClient wompiRestClient;
    private final PaymentStrategyFactory strategyFactory;

    @Value("${wompi.public-key}")
    private String publicKey;

    @Value("${wompi.integrity-key}")
    private String integrityKey;

    /**
     * Creates a transaction in Wompi for the given payment.
     *
     * @return Wompi transaction data with id, status and (for PSE) redirect_url
     */
    public WompiTransactionResponse.TransactionData charge(Payment payment, OrderEvent event) {
        WompiMerchantResponse.MerchantData merchantData = fetchMerchantData();
        String acceptanceToken = merchantData.getPresignedAcceptance().getAcceptanceToken();
        String personalDataAuth = merchantData.getPresignedPersonalDataAuth().getAcceptanceToken();
        PaymentMethodStrategy strategy = strategyFactory.getStrategy(payment.getPaymentMethod());

        long amountInCents = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // sumar 141000 a amountInCents para pruebas de PSE, Nequi y Daviplata
        amountInCents += 141000;

        log.info("Charging order {} with amount {} cents", payment.getOrderId(), amountInCents);

        String currency = event.getCurrency() != null ? event.getCurrency() : "COP";
        String reference = payment.getOrderId().toString();
        String integritySignature = buildIntegritySignature(reference, amountInCents, currency);

        WompiCreateTransactionRequest request = WompiCreateTransactionRequest.builder()
                .amountInCents(amountInCents)
                .currency(currency)
                .customerEmail(event.getCustomerEmail())
                .paymentMethod(strategy.buildPaymentMethodData(event))
                .reference(reference)
                .acceptanceToken(acceptanceToken)
                .acceptancePersonalAuth(personalDataAuth)
                .integritySignature(integritySignature)
                .redirectUrl(event.getRedirectUrl())
                .build();

        log.info("Sending transaction to Wompi for order {} via {}", payment.getOrderId(), payment.getPaymentMethod());

        WompiTransactionResponse response = wompiRestClient.post()
                .uri("/transactions")
                .body(request)
                .retrieve()
                .body(WompiTransactionResponse.class);

        if (response == null || response.getData() == null) {
            throw new WompiException("Empty response from Wompi for order: " + payment.getOrderId());
        }

        log.info("Wompi transaction {} status {} for order {}",
                response.getData().getId(), response.getData().getStatus(), payment.getOrderId());

        return response.getData();
    }

    private String buildIntegritySignature(String reference, long amountInCents, String currency) {
        String raw = reference + amountInCents + currency + integrityKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new WompiException("Could not generate integrity signature");
        }
    }

    private WompiMerchantResponse.MerchantData fetchMerchantData() {
        WompiMerchantResponse merchant = wompiRestClient.get()
                .uri("/merchants/{publicKey}", publicKey)
                .retrieve()
                .body(WompiMerchantResponse.class);

        if (merchant == null || merchant.getData() == null
                || merchant.getData().getPresignedAcceptance() == null
                || merchant.getData().getPresignedPersonalDataAuth() == null) {
            throw new WompiException("Could not fetch acceptance tokens from Wompi");
        }

        return merchant.getData();
    }
}
