package com.rideflow.demo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rideflow.demo.api.dto.auth.LoginRequest;
import com.rideflow.demo.api.dto.auth.LogoutRequest;
import com.rideflow.demo.api.dto.auth.PasswordResetConfirmRequest;
import com.rideflow.demo.api.dto.auth.PasswordResetRequest;
import com.rideflow.demo.api.dto.auth.RegisterRequest;
import com.rideflow.demo.api.dto.auth.TokenRefreshRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.RefreshToken;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.RefreshTokenRepository;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.JwtService;
import com.rideflow.demo.security.RideFlowUserPrincipal;
import com.rideflow.demo.security.RideFlowUserDetailsService;
import com.rideflow.demo.security.TokenType;
import com.rideflow.demo.service.AuditLogWriter;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RideFlowUserDetailsService userDetailsService;

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerCreatesCustomerAndIssuesTokens() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.id = 15L;
            return user;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(86400L);

        var response = authService.register(
            new RegisterRequest("New User", " NEW@EXAMPLE.COM ", "Password1", " +212600000000 ")
        );

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().role()).isEqualTo(UserRole.CUSTOMER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("Existing User", "existing@example.com", "Password1", null)
        ))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("An account with this email already exists.");
    }

    @Test
    void logoutRevokesExistingRefreshToken() {
        User user = new User();
        user.id = 9L;
        user.email = "user@example.com";
        user.role = UserRole.CUSTOMER;
        user.status = UserStatus.ACTIVE;

        RefreshToken token = new RefreshToken();
        token.id = 22L;
        token.user = user;
        token.token = "refresh-token";
        token.expiresAt = Instant.now().plusSeconds(600);

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        authService.logout(new LogoutRequest("refresh-token"));

        assertThat(token.revokedAt).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void loginRejectsDisabledUsersBeforeAuthentication() {
        User user = user("disabled@example.com", UserStatus.DISABLED);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("disabled@example.com", "Password1")))
            .isInstanceOf(DisabledException.class)
            .hasMessage("Your account is disabled. Contact an administrator.");
    }

    @Test
    void refreshRevokesOldTokenAndIssuesNewTokens() {
        User user = user("refresh@example.com", UserStatus.ACTIVE);
        user.id = 21L;
        user.preferredPaymentMethod = PaymentMethod.WALLET;

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.id = 31L;
        refreshToken.user = user;
        refreshToken.token = "refresh-token";
        refreshToken.expiresAt = Instant.now().plusSeconds(600);

        RideFlowUserPrincipal principal = new RideFlowUserPrincipal(
            user.id,
            user.email,
            user.passwordHash,
            user.fullName,
            user.role,
            user.status
        );

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.isTokenOfType("refresh-token", TokenType.REFRESH)).thenReturn(true);
        when(userDetailsService.loadUserById(user.id)).thenReturn(principal);
        when(jwtService.isTokenValid("refresh-token", principal, TokenType.REFRESH)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(86400L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.refresh(new TokenRefreshRequest("refresh-token"));

        assertThat(refreshToken.revokedAt).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(jwtService).isTokenOfType("refresh-token", TokenType.REFRESH);
        verify(jwtService).isTokenValid("refresh-token", principal, TokenType.REFRESH);
    }

    @Test
    void refreshRejectsExpiredTokens() {
        User user = user("expired@example.com", UserStatus.ACTIVE);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.token = "expired-token";
        refreshToken.expiresAt = Instant.now().minusSeconds(30);

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest("expired-token")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Refresh token is no longer valid.");
    }

    @Test
    void passwordResetEndpointsRemainUnimplemented() {
        assertThatThrownBy(() -> authService.requestPasswordReset(new PasswordResetRequest("user@example.com")))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Password reset is not implemented yet.");

        assertThatThrownBy(() -> authService.confirmPasswordReset(
            new PasswordResetConfirmRequest("token", "Password1")
        ))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Password reset confirmation is not implemented yet.");
    }

    private User user(String email, UserStatus status) {
        User user = new User();
        user.id = 11L;
        user.email = email;
        user.passwordHash = "hash";
        user.fullName = "User";
        user.role = UserRole.CUSTOMER;
        user.status = status;
        return user;
    }
}
