package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.user.ProfileUpdateRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Test
    void getCurrentProfileReturnsAuthenticatedUser() {
        User user = user(3L, "user@example.com");
        when(authenticatedUserService.getCurrentUserId()).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        var response = userProfileService.getCurrentProfile();

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.WALLET);
    }

    @Test
    void updateCurrentProfileNormalizesFieldsAndStoresPaymentMethod() {
        User user = user(3L, "user@example.com");
        when(authenticatedUserService.getCurrentUserId()).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var response = userProfileService.updateCurrentProfile(
            new ProfileUpdateRequest(
                "Updated User",
                " UPDATED@EXAMPLE.COM ",
                "  +212600000000  ",
                PaymentMethod.DEBIT_CARD
            )
        );

        assertThat(response.email()).isEqualTo("updated@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+212600000000");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.DEBIT_CARD);
    }

    @Test
    void updateCurrentProfileRejectsEmailAlreadyUsedByAnotherAccount() {
        User user = user(3L, "user@example.com");
        when(authenticatedUserService.getCurrentUserId()).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.updateCurrentProfile(
            new ProfileUpdateRequest("Updated User", "taken@example.com", null, PaymentMethod.CREDIT_CARD)
        ))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("An account with this email already exists.");
    }

    private User user(Long id, String email) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.fullName = "Profile User";
        user.phoneNumber = "+212611111111";
        user.preferredPaymentMethod = PaymentMethod.WALLET;
        user.role = UserRole.CUSTOMER;
        user.status = UserStatus.ACTIVE;
        return user;
    }
}
