package org.example.paymentservice.dto.wompi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WompiMerchantResponse {

    private MerchantData data;

    @Data
    public static class MerchantData {
        @JsonProperty("presigned_acceptance")
        private PresignedAcceptance presignedAcceptance;

        @JsonProperty("presigned_personal_data_auth")
        private PresignedAcceptance presignedPersonalDataAuth;
    }

    @Data
    public static class PresignedAcceptance {
        @JsonProperty("acceptance_token")
        private String acceptanceToken;
        private String permalink;
    }
}