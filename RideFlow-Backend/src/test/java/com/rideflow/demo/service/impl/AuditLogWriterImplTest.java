package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.AuditStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.AuditLog;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.AuditLogRepository;
import com.rideflow.demo.security.RideFlowUserPrincipal;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditLogWriterImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuditLogWriterImpl auditLogWriter;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logSuccessPersistsCustomerAuditLogWithRequestMetadata() {
        RideFlowUserPrincipal principal = new RideFlowUserPrincipal(
            7L,
            "customer@example.com",
            "hash",
            "Customer",
            UserRole.CUSTOMER,
            UserStatus.ACTIVE
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        User userReference = new User();
        userReference.id = 7L;
        when(entityManager.getReference(User.class, 7L)).thenReturn(userReference);

        auditLogWriter.logSuccess(
            AuditActionType.RENTAL_UNLOCK,
            AuditEntityType.RENTAL,
            11L,
            Map.of("scooterId", 3L, "battery", 82)
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.actorUser.id).isEqualTo(7L);
        assertThat(saved.status).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.ipAddress).isEqualTo("203.0.113.9");
        assertThat(saved.userAgent).isEqualTo("JUnit");
        assertThat(saved.payload).contains("\"scooterId\":3");
    }

    @Test
    void logSuccessSkipsAdminActors() {
        RideFlowUserPrincipal principal = new RideFlowUserPrincipal(
            1L,
            "admin@example.com",
            "hash",
            "Admin",
            UserRole.ADMIN,
            UserStatus.ACTIVE
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        auditLogWriter.logSuccess(
            AuditActionType.USER_STATUS_CHANGE,
            AuditEntityType.USER,
            99L,
            "ignored"
        );

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void logFailureWithExplicitCustomerSerializesCollectionsAndUsesRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("User-Agent", "JUnit-Agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        User actor = new User();
        actor.id = 25L;
        actor.role = UserRole.CUSTOMER;
        when(entityManager.getReference(User.class, 25L)).thenReturn(actor);

        auditLogWriter.logFailure(
            actor,
            null,
            AuditActionType.PAYMENT_FAILED,
            AuditEntityType.PAYMENT,
            91L,
            Map.of("attempts", 2, "reasons", new String[] { "gateway", "timeout" })
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.status).isEqualTo(AuditStatus.FAILED);
        assertThat(saved.actorUser.id).isEqualTo(25L);
        assertThat(saved.actorRole).isEqualTo(com.rideflow.demo.domain.enums.AuditActorRole.CUSTOMER);
        assertThat(saved.ipAddress).isEqualTo("198.51.100.7");
        assertThat(saved.userAgent).isEqualTo("JUnit-Agent");
        assertThat(saved.payload).contains("\"attempts\":2");
        assertThat(saved.payload).contains("\"reasons\":[\"gateway\",\"timeout\"]");
    }

    @Test
    void logFailureInNewTransactionFallsBackWhenPayloadSerializationFails() {
        Object badValue = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };

        auditLogWriter.logFailureInNewTransaction(
            AuditActionType.SCOOTER_UPDATE,
            AuditEntityType.SCOOTER,
            14L,
            new Object[] { badValue }
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.status).isEqualTo(AuditStatus.FAILED);
        assertThat(saved.actorRole).isEqualTo(com.rideflow.demo.domain.enums.AuditActorRole.SYSTEM);
        assertThat(saved.payload).contains("serializationError");
    }
}
