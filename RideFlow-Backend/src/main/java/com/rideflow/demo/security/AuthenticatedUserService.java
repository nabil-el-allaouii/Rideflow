package com.rideflow.demo.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    public RideFlowUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof RideFlowUserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required.");
        }

        return principal;
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public boolean isAdmin() {
        return getCurrentUser().getRole().name().equals("ADMIN");
    }
}
