package com.rideflow.demo.api.dto.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    AuthUserResponse user
) {
}
