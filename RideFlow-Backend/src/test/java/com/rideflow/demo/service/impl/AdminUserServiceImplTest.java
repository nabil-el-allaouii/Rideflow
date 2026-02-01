package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.user.UserFilterRequest;
import com.rideflow.demo.api.dto.user.UserStatusUpdateRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.AuditLogWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void findAllReturnsPagedMappedUsers() {
        User user = user(4L, "customer@example.com", UserStatus.ACTIVE);
        when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user)));

        var page = adminUserService.findAll(new UserFilterRequest("customer", UserRole.CUSTOMER, UserStatus.ACTIVE, null, null, -1, 200));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(userRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), pageableCaptor.capture());
        assertThat(page.content()).hasSize(1);
        assertThat(page.page()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(page.content().get(0).email()).isEqualTo("customer@example.com");
    }

    @Test
    void updateStatusRejectsSuspendingCurrentAdminAccount() {
        User admin = user(10L, "admin@example.com", UserStatus.ACTIVE);
        admin.role = UserRole.ADMIN;

        when(authenticatedUserService.getCurrentUserId()).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminUserService.updateStatus(10L, new UserStatusUpdateRequest(UserStatus.SUSPENDED)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("You cannot suspend or disable your own account.");
    }

    @Test
    void updateStatusPersistsChangeAndWritesAuditEntry() {
        User customer = user(4L, "customer@example.com", UserStatus.ACTIVE);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(10L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(customer));
        when(userRepository.save(customer)).thenReturn(customer);

        var response = adminUserService.updateStatus(4L, new UserStatusUpdateRequest(UserStatus.SUSPENDED));

        assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
        verify(auditLogWriter).logSuccess(any(), any(), any(), any());
    }

    private User user(Long id, String email, UserStatus status) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.fullName = "User " + id;
        user.phoneNumber = "+212600000000";
        user.preferredPaymentMethod = PaymentMethod.CREDIT_CARD;
        user.role = UserRole.CUSTOMER;
        user.status = status;
        user.createdAt = Instant.now();
        return user;
    }
}
