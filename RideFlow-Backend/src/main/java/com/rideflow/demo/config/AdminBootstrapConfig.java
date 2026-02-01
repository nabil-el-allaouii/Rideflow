package com.rideflow.demo.config;

import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.UserRepository;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    CommandLineRunner adminBootstrapRunner(
        AdminBootstrapProperties adminBootstrapProperties,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!adminBootstrapProperties.isEnabled()) {
                return;
            }

            String normalizedEmail = adminBootstrapProperties.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

            if (userRepository.existsByEmail(normalizedEmail)) {
                return;
            }

            User adminUser = new User();
            adminUser.email = normalizedEmail;
            adminUser.passwordHash = passwordEncoder.encode(adminBootstrapProperties.getPassword());
            adminUser.fullName = adminBootstrapProperties.getFullName().trim();
            adminUser.phoneNumber = normalizeOptional(adminBootstrapProperties.getPhoneNumber());
            adminUser.role = UserRole.ADMIN;
            adminUser.status = UserStatus.ACTIVE;

            userRepository.save(adminUser);
        };
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
