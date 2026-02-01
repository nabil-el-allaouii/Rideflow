package com.rideflow.demo.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters.")
    String fullName,
    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    String email,
    @NotBlank(message = "Password is required.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters and contain uppercase, lowercase, and a number."
    )
    String password,
    @Size(max = 30, message = "Phone number must not exceed 30 characters.")
    String phoneNumber
) {
}
