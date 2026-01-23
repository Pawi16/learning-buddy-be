package dev.pawin.backend_learning_buddy.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    @JsonProperty("token")
    private String accessToken;

    @JsonProperty("token_type")
    @Builder.Default // 1. Automatically sets this to "Bearer"
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private long expiresIn; // 2. In Seconds (Standard)
}