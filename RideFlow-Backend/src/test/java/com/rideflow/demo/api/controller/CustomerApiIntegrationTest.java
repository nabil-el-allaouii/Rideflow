package com.rideflow.demo.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rideflow.demo.domain.enums.PaymentMethod;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import com.rideflow.demo.domain.enums.RentalStatus;
import com.rideflow.demo.domain.enums.ScooterStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.Receipt;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.model.Scooter;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.PaymentRepository;
import com.rideflow.demo.domain.repository.ReceiptRepository;
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
class CustomerApiIntegrationTest {

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
    private ReceiptRepository receiptRepository;

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
    void currentUserProfileCanBeFetchedAndUpdated() throws Exception {
        User customer = userRepository.save(user("customer@example.com"));

        mockMvc.perform(get("/api/users/me").with(authentication(customerAuth(customer))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("customer@example.com"))
            .andExpect(jsonPath("$.paymentMethod").value("WALLET"));

        mockMvc.perform(put("/api/users/me")
                .with(authentication(customerAuth(customer)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Updated Customer",
                      "email": "updated.customer@example.com",
                      "phoneNumber": "+212611222333",
                      "paymentMethod": "DEBIT_CARD"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("updated.customer@example.com"))
            .andExpect(jsonPath("$.paymentMethod").value("DEBIT_CARD"));
    }

    @Test
    void receiptEndpointsReturnOwnedReceiptArtifacts() throws Exception {
        User customer = userRepository.save(user("customer@example.com"));
        Scooter scooter = scooterRepository.save(scooter());
        Rental rental = rentalRepository.save(rental(customer, scooter));

        Payment unlock = payment(customer, rental, PaymentType.UNLOCK_FEE, new BigDecimal("1.00"), "TX-U-1");
        Payment finalPayment = payment(customer, rental, PaymentType.FINAL_PAYMENT, new BigDecimal("1.50"), "TX-F-1");
        paymentRepository.save(unlock);
        paymentRepository.save(finalPayment);

        Receipt receipt = new Receipt();
        receipt.receiptCode = "RCPT-100";
        receipt.rental = rental;
        receipt.generatedAt = Instant.now();
        receipt.unlockFeeCharged = new BigDecimal("1.00");
        receipt.usageCost = new BigDecimal("1.50");
        receipt.totalCost = new BigDecimal("2.50");
        receiptRepository.save(receipt);

        mockMvc.perform(get("/api/receipts/{rentalId}", rental.id).with(authentication(customerAuth(customer))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receiptCode").value("RCPT-100"))
            .andExpect(jsonPath("$.scooterCode").value("RF-123"))
            .andExpect(jsonPath("$.unlockPaymentReference").value("TX-U-1"))
            .andExpect(jsonPath("$.finalPaymentReference").value("TX-F-1"));

        mockMvc.perform(get("/api/receipts/{rentalId}/html", rental.id).with(authentication(customerAuth(customer))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("RideFlow Receipt")));

        mockMvc.perform(get("/api/receipts/{rentalId}/pdf", rental.id).with(authentication(customerAuth(customer))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    private UsernamePasswordAuthenticationToken customerAuth(User user) {
        RideFlowUserPrincipal principal = new RideFlowUserPrincipal(
            user.id,
            user.email,
            user.passwordHash,
            user.fullName,
            user.role,
            user.status
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private User user(String email) {
        User user = new User();
        user.email = email;
        user.passwordHash = "hash";
        user.fullName = "Customer";
        user.phoneNumber = "+212600000000";
        user.preferredPaymentMethod = PaymentMethod.WALLET;
        user.role = UserRole.CUSTOMER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    private Scooter scooter() {
        Scooter scooter = new Scooter();
        scooter.publicCode = "RF-123";
        scooter.model = "City";
        scooter.batteryPercentage = 77;
        scooter.status = ScooterStatus.AVAILABLE;
        scooter.kilometersTraveled = new BigDecimal("10.00");
        scooter.lastActivityAt = Instant.now();
        return scooter;
    }

    private Rental rental(User user, Scooter scooter) {
        Rental rental = new Rental();
        rental.user = user;
        rental.scooter = scooter;
        rental.status = RentalStatus.COMPLETED;
        rental.startTime = Instant.now().minusSeconds(600);
        rental.endTime = Instant.now();
        rental.durationMinutes = 10;
        rental.batteryAtStart = 77;
        rental.batteryAtEnd = 75;
        rental.distanceTraveled = new BigDecimal("3.50");
        rental.unlockFeeApplied = new BigDecimal("1.00");
        rental.ratePerMinuteApplied = new BigDecimal("0.15");
        rental.totalCost = new BigDecimal("2.50");
        return rental;
    }

    private Payment payment(User user, Rental rental, PaymentType type, BigDecimal amount, String reference) {
        Payment payment = new Payment();
        payment.user = user;
        payment.rental = rental;
        payment.type = type;
        payment.amount = amount;
        payment.status = PaymentStatus.SUCCEEDED;
        payment.paymentMethod = PaymentMethod.WALLET;
        payment.transactionReference = reference;
        return payment;
    }
}
