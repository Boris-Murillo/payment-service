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

@Slf4j
@Service
@RequiredArgsConstructor
public class WompiService {

    private final RestClient wompiRestClient;
    private final PaymentStrategyFactory strategyFactory;

    @Value("${wompi.public-key}")
    private String publicKey;

    /**
     * Creates a transaction in Wompi for the given payment.
     *
     * @return Wompi transaction data with id, status and (for PSE) redirect_url
     */
    public WompiTransactionResponse.TransactionData charge(Payment payment, OrderEvent event) {
        String acceptanceToken = fetchAcceptanceToken();
        PaymentMethodStrategy strategy = strategyFactory.getStrategy(payment.getPaymentMethod());

        long amountInCents = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        WompiCreateTransactionRequest request = WompiCreateTransactionRequest.builder()
                .amountInCents(amountInCents)
                .currency(event.getCurrency() != null ? event.getCurrency() : "COP")
                .customerEmail(event.getCustomerEmail())
                .paymentMethod(strategy.buildPaymentMethodData(event))
                .reference(payment.getOrderId().toString())
                .acceptanceToken(acceptanceToken)
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

    private String fetchAcceptanceToken() {
        WompiMerchantResponse merchant = wompiRestClient.get()
                .uri("/merchants/{publicKey}", publicKey)
                .retrieve()
                .body(WompiMerchantResponse.class);

        if (merchant == null || merchant.getData() == null
                || merchant.getData().getPresignedAcceptance() == null) {
            throw new WompiException("Could not fetch acceptance token from Wompi");
        }

        return merchant.getData().getPresignedAcceptance().getAcceptanceToken();
    }
}
