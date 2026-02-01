package com.rideflow.demo.domain.model;

import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email")
    }
)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    public String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    public String fullName;

    @Column(name = "phone_number", length = 30)
    public String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_payment_method", length = 30)
    public PaymentMethod preferredPaymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    public UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public UserStatus status;

    @Column(name = "last_login_at")
    public Instant lastLoginAt;

    @OneToMany(mappedBy = "user")
    public List<Rental> rentals;

    @OneToMany(mappedBy = "user")
    public List<Payment> payments;

    @OneToMany(mappedBy = "actorUser")
    public List<AuditLog> auditLogs;

    @OneToMany(mappedBy = "user")
    public List<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "user")
    public List<PasswordResetToken> passwordResetTokens;
}
