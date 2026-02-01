package com.rideflow.demo.api.dto.auth;

public record PasswordResetConfirmRequest(
    String token,
    String newPassword
) {
}
