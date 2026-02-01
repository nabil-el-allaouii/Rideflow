package com.rideflow.demo.api.dto.user;

import com.rideflow.demo.domain.enums.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters.")
    String fullName,
    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    String email,
    @Size(max = 30, message = "Phone number must not exceed 30 characters.")
    @Pattern(
        regexp = "^\\+?[0-9()\\-\\s]{0,30}$",
        message = "Phone number contains invalid characters."
    )
    String phoneNumber,
    PaymentMethod paymentMethod
) {
}
