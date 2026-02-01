package com.rideflow.demo.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.AuditStatus;
import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.AuditLog;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.PricingConfig;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.AuditLogRepository;
import com.rideflow.demo.domain.repository.PaymentRepository;
import com.rideflow.demo.domain.repository.PricingConfigRepository;
import com.rideflow.demo.domain.repository.RentalRepository;
import com.rideflow.demo.domain.repository.ScooterRepository;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.RideFlowUserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScooterRepository scooterRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PricingConfigRepository pricingConfigRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE audit_logs");
        jdbcTemplate.execute("TRUNCATE TABLE receipts");
        jdbcTemplate.execute("TRUNCATE TABLE payments");
        jdbcTemplate.execute("TRUNCATE TABLE rentals");
        jdbcTemplate.execute("TRUNCATE TABLE scooters");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE password_reset_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE pricing_configs");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void pricingEndpointsReadAndUpdateActivePricing() throws Exception {
        PricingConfig current = pricing("1.00", "0.15", "0.50");
        pricingConfigRepository.save(current);

        mockMvc.perform(get("/api/admin/pricing/current").with(authentication(adminAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unlockFee").value(1.00))
            .andExpect(jsonPath("$.ratePerMinute").value(0.15))
            .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(put("/api/admin/pricing")
                .with(authentication(adminAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "unlockFee": 1.50,
                      "ratePerMinute": 0.20,
                      "batteryConsumptionRate": 0.75,
                      "currency": "USD"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unlockFee").value(1.50))
            .andExpect(jsonPath("$.ratePerMinute").value(0.20))
            .andExpect(jsonPath("$.batteryConsumptionRate").value(0.75))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void statisticsUsersAndAuditLogsEndpointsReturnLiveData() throws Exception {
        User customer = user("customer@example.com", UserRole.CUSTOMER, UserStatus.ACTIVE);
        customer = userRepository.save(customer);

        User suspended = user("suspended@example.com", UserRole.CUSTOMER, UserStatus.SUSPENDED);
        suspended = userRepository.save(suspended);

        Scooter available = scooter("RF-001", ScooterStatus.AVAILABLE, 82);
        available = scooterRepository.save(available);

        Scooter inUse = scooter("RF-002", ScooterStatus.IN_USE, 63);
        inUse = scooterRepository.save(inUse);

        Rental activeRental = rental(customer, inUse, RentalStatus.ACTIVE, new BigDecimal("4.20"));
        rentalRepository.save(activeRental);

        Payment payment = new Payment();
        payment.user = customer;
        payment.rental = activeRental;
        payment.type = com.rideflow.demo.domain.enums.PaymentType.FINAL_PAYMENT;
        payment.amount = new BigDecimal("4.20");
        payment.status = com.rideflow.demo.domain.enums.PaymentStatus.SUCCEEDED;
        payment.paymentMethod = PaymentMethod.CREDIT_CARD;
        payment.transactionReference = "TX-STAT-1";
        paymentRepository.save(payment);

        AuditLog customerLog = new AuditLog();
        customerLog.actorUser = customer;
        customerLog.actorRole = AuditActorRole.CUSTOMER;
        customerLog.actionType = AuditActionType.RENTAL_END;
        customerLog.entityType = AuditEntityType.RENTAL;
        customerLog.entityId = activeRental.id;
        customerLog.payload = "{\"rentalId\":" + activeRental.id + "}";
        customerLog.status = AuditStatus.SUCCESS;
        auditLogRepository.save(customerLog);

        AuditLog adminLog = new AuditLog();
        adminLog.actorUser = userRepository.save(user("admin-log@example.com", UserRole.ADMIN, UserStatus.ACTIVE));
        adminLog.actorRole = AuditActorRole.ADMIN;
        adminLog.actionType = AuditActionType.USER_STATUS_CHANGE;
        adminLog.entityType = AuditEntityType.USER;
        adminLog.entityId = suspended.id;
        adminLog.payload = "{\"status\":\"SUSPENDED\"}";
        adminLog.status = AuditStatus.SUCCESS;
        auditLogRepository.save(adminLog);

        mockMvc.perform(get("/api/admin/statistics").with(authentication(adminAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalScooters").value(2))
            .andExpect(jsonPath("$.availableScooters").value(1))
            .andExpect(jsonPath("$.inUseScooters").value(1))
            .andExpect(jsonPath("$.activeRentals").value(1))
            .andExpect(jsonPath("$.totalUsers").value(2));

        mockMvc.perform(get("/api/admin/users?page=0&size=10").with(authentication(adminAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").exists())
            .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/admin/audit-logs?page=0&size=10").with(authentication(adminAuth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].actorUserEmail").value("customer@example.com"));
    }

    @Test
    void adminCanUpdateUserStatusThroughApi() throws Exception {
        User customer = userRepository.save(user("member@example.com", UserRole.CUSTOMER, UserStatus.ACTIVE));

        mockMvc.perform(put("/api/admin/users/{id}/status", customer.id)
                .with(authentication(adminAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("member@example.com"))
            .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void auditLogsEndpointAppliesQueryAndStatusFilters() throws Exception {
        User customer = userRepository.save(user("filters@example.com", UserRole.CUSTOMER, UserStatus.ACTIVE));

        AuditLog failedPayment = new AuditLog();
        failedPayment.actorUser = customer;
        failedPayment.actorRole = AuditActorRole.CUSTOMER;
        failedPayment.actionType = AuditActionType.PAYMENT_FAILED;
        failedPayment.entityType = AuditEntityType.PAYMENT;
        failedPayment.entityId = 55L;
        failedPayment.payload = "{\"reason\":\"timeout\"}";
        failedPayment.ipAddress = "203.0.113.10";
        failedPayment.userAgent = "JUnit";
        failedPayment.status = AuditStatus.FAILED;
        auditLogRepository.save(failedPayment);

        AuditLog successfulRental = new AuditLog();
        successfulRental.actorUser = customer;
        successfulRental.actorRole = AuditActorRole.CUSTOMER;
        successfulRental.actionType = AuditActionType.RENTAL_END;
        successfulRental.entityType = AuditEntityType.RENTAL;
        successfulRental.entityId = 77L;
        successfulRental.payload = "{\"note\":\"completed\"}";
        successfulRental.status = AuditStatus.SUCCESS;
        auditLogRepository.save(successfulRental);

        mockMvc.perform(get("/api/admin/audit-logs")
                .with(authentication(adminAuth()))
                .param("actorUserId", customer.id.toString())
                .param("actionType", "PAYMENT_FAILED")
                .param("entityType", "PAYMENT")
                .param("status", "FAILED")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].actionType").value("PAYMENT_FAILED"))
            .andExpect(jsonPath("$.content[0].entityType").value("PAYMENT"))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        RideFlowUserPrincipal principal = new RideFlowUserPrincipal(
            900L,
            "admin@example.com",
            "hash",
            "Admin",
            UserRole.ADMIN,
            UserStatus.ACTIVE
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private User user(String email, UserRole role, UserStatus status) {
        User user = new User();
        user.email = email;
        user.passwordHash = "hash";
        user.fullName = email;
        user.phoneNumber = "+212600000000";
        user.preferredPaymentMethod = PaymentMethod.CREDIT_CARD;
        user.role = role;
        user.status = status;
        return user;
    }

    private Scooter scooter(String publicCode, ScooterStatus status, int battery) {
        Scooter scooter = new Scooter();
        scooter.publicCode = publicCode;
        scooter.model = "Urban";
        scooter.batteryPercentage = battery;
        scooter.status = status;
        scooter.kilometersTraveled = BigDecimal.ZERO;
        scooter.lastActivityAt = Instant.now();
        return scooter;
    }

    private Rental rental(User user, Scooter scooter, RentalStatus status, BigDecimal totalCost) {
        Rental rental = new Rental();
        rental.user = user;
        rental.scooter = scooter;
        rental.status = status;
        rental.startTime = Instant.now().minusSeconds(600);
        rental.endTime = Instant.now();
        rental.durationMinutes = 10;
        rental.batteryAtStart = scooter.batteryPercentage;
        rental.batteryAtEnd = Math.max(0, scooter.batteryPercentage - 5);
        rental.distanceTraveled = new BigDecimal("3.50");
        rental.unlockFeeApplied = new BigDecimal("1.00");
        rental.ratePerMinuteApplied = new BigDecimal("0.15");
        rental.totalCost = totalCost;
        return rental;
    }

    private PricingConfig pricing(String unlockFee, String ratePerMinute, String batteryRate) {
        PricingConfig pricing = new PricingConfig();
        pricing.unlockFee = new BigDecimal(unlockFee);
        pricing.ratePerMinute = new BigDecimal(ratePerMinute);
        pricing.batteryConsumptionRate = new BigDecimal(batteryRate);
        pricing.currency = "USD";
        pricing.active = true;
        pricing.effectiveFrom = Instant.now();
        return pricing;
    }
}
