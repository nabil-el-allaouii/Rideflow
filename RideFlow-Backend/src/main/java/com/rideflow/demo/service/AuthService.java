package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.auth.AuthResponse;
import com.rideflow.demo.api.dto.auth.LoginRequest;
import com.rideflow.demo.api.dto.auth.LogoutRequest;
import com.rideflow.demo.api.dto.auth.PasswordResetConfirmRequest;
import com.rideflow.demo.api.dto.auth.PasswordResetRequest;
import com.rideflow.demo.api.dto.auth.RegisterRequest;
import com.rideflow.demo.api.dto.auth.TokenRefreshRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(TokenRefreshRequest request);
    void logout(LogoutRequest request);
    void requestPasswordReset(PasswordResetRequest request);
    void confirmPasswordReset(PasswordResetConfirmRequest request);
}
