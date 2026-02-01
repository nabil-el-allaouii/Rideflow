package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.auth.AuthResponse;
import com.rideflow.demo.api.dto.auth.AuthUserResponse;
import com.rideflow.demo.api.dto.auth.LoginRequest;
import com.rideflow.demo.api.dto.auth.LogoutRequest;
import com.rideflow.demo.api.dto.auth.PasswordResetConfirmRequest;
import com.rideflow.demo.api.dto.auth.PasswordResetRequest;
import com.rideflow.demo.api.dto.auth.RegisterRequest;
import com.rideflow.demo.api.dto.auth.TokenRefreshRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.enums.AuditEntityType;
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
import com.rideflow.demo.service.AuthService;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RideFlowUserDetailsService userDetailsService;
    private final AuditLogWriter auditLogWriter;

    public AuthServiceImpl(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RideFlowUserDetailsService userDetailsService,
        AuditLogWriter auditLogWriter
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        try {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new BusinessRuleException("An account with this email already exists.");
            }

            User user = new User();
            user.email = normalizedEmail;
            user.passwordHash = passwordEncoder.encode(request.password());
            user.fullName = request.fullName().trim();
            user.phoneNumber = normalizeOptional(request.phoneNumber());
            user.role = UserRole.CUSTOMER;
            user.status = UserStatus.ACTIVE;

            User savedUser = userRepository.save(user);
            AuthResponse response = issueTokens(savedUser);
            auditLogWriter.logSuccess(
                savedUser,
                resolveAuditActorRole(savedUser),
                AuditActionType.REGISTER,
                AuditEntityType.USER,
                savedUser.id,
                payload("email", savedUser.email, "role", savedUser.role, "status", savedUser.status)
            );
            return response;
        } catch (RuntimeException exception) {
            auditLogWriter.logFailureInNewTransaction(
                null,
                AuditActorRole.SYSTEM,
                AuditActionType.REGISTER,
                AuditEntityType.USER,
                null,
                payload("email", normalizedEmail, "reason", exception.getMessage())
            );
            throw exception;
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        try {
            if (user == null) {
                throw new BadCredentialsException("Invalid email or password.");
            }

            assertUserCanAuthenticate(user);

            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );

            user.lastLoginAt = Instant.now();
            User savedUser = userRepository.save(user);
            AuthResponse response = issueTokens(savedUser);
            auditLogWriter.logSuccess(
                savedUser,
                resolveAuditActorRole(savedUser),
                AuditActionType.LOGIN,
                AuditEntityType.AUTH,
                savedUser.id,
                payload("email", savedUser.email, "status", savedUser.status)
            );
            return response;
        } catch (RuntimeException exception) {
            auditLogWriter.logFailureInNewTransaction(
                user,
                user == null ? AuditActorRole.SYSTEM : resolveAuditActorRole(user),
                AuditActionType.LOGIN,
                AuditEntityType.AUTH,
                user == null ? null : user.id,
                payload("email", normalizedEmail, "reason", exception.getMessage())
            );
            throw exception;
        }
    }

    @Override
    public AuthResponse refresh(TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new BadCredentialsException("Refresh token is invalid."));

        if (refreshToken.revokedAt != null || refreshToken.expiresAt.isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token is no longer valid.");
        }

        try {
            if (!jwtService.isTokenOfType(refreshToken.token, TokenType.REFRESH)) {
                throw new BadCredentialsException("Refresh token is invalid.");
            }
        } catch (JwtException exception) {
            throw new BadCredentialsException("Refresh token is invalid.");
        }

        User user = refreshToken.user;
        assertUserCanAuthenticate(user);

        RideFlowUserPrincipal principal =
            (RideFlowUserPrincipal) userDetailsService.loadUserById(user.id);

        if (!jwtService.isTokenValid(refreshToken.token, principal, TokenType.REFRESH)) {
            throw new BadCredentialsException("Refresh token is invalid.");
        }

        refreshToken.revokedAt = Instant.now();
        refreshTokenRepository.save(refreshToken);

        return issueTokens(user);
    }

    @Override
    public void logout(LogoutRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken()).orElse(null);
        User actorUser = token == null ? null : token.user;

        if (token != null && token.revokedAt == null) {
            token.revokedAt = Instant.now();
            refreshTokenRepository.save(token);
        }

        auditLogWriter.logSuccess(
            actorUser,
            actorUser == null ? AuditActorRole.SYSTEM : resolveAuditActorRole(actorUser),
            AuditActionType.LOGOUT,
            AuditEntityType.AUTH,
            actorUser == null ? null : actorUser.id,
            payload(
                "refreshTokenProvided",
                request.refreshToken() != null && !request.refreshToken().isBlank()
            )
        );
    }

    @Override
    public void requestPasswordReset(PasswordResetRequest request) {
        throw new UnsupportedOperationException("Password reset is not implemented yet.");
    }

    @Override
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        throw new UnsupportedOperationException("Password reset confirmation is not implemented yet.");
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.token = refreshTokenValue;
        refreshToken.expiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds());
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken,
            refreshTokenValue,
            "Bearer",
            jwtService.getAccessTokenExpirationSeconds(),
            new AuthUserResponse(
                user.id,
                user.email,
                user.fullName,
                user.phoneNumber,
                user.preferredPaymentMethod,
                user.role,
                user.status
            )
        );
    }

    private void assertUserCanAuthenticate(User user) {
        if (user.status == UserStatus.SUSPENDED) {
            throw new LockedException("Your account is suspended. Contact an administrator.");
        }

        if (user.status == UserStatus.DISABLED) {
            throw new DisabledException("Your account is disabled. Contact an administrator.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AuditActorRole resolveAuditActorRole(User user) {
        return user.role == UserRole.ADMIN ? AuditActorRole.ADMIN : AuditActorRole.CUSTOMER;
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            payload.put(String.valueOf(values[index]), values[index + 1]);
        }
        return payload;
    }
}
