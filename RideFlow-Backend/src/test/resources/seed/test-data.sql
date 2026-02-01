-- RideFlow sample test data
-- Compatible with the current schema for local MySQL/H2 testing.
-- Seeded password for all users below: Password123!

DELETE FROM audit_logs;
DELETE FROM receipts;
DELETE FROM payments;
DELETE FROM rentals;
DELETE FROM scooters;
DELETE FROM pricing_configs;
DELETE FROM refresh_tokens;
DELETE FROM password_reset_tokens;
DELETE FROM users;

INSERT INTO pricing_configs (
    id,
    created_at,
    updated_at,
    version,
    unlock_fee,
    rate_per_minute,
    battery_consumption_rate,
    currency,
    effective_from,
    is_active
) VALUES
    (1, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, 1.00, 0.20, 0.50, 'USD', '2026-03-25 08:00:00', true);

INSERT INTO users (
    id,
    created_at,
    updated_at,
    version,
    last_login_at,
    phone_number,
    full_name,
    email,
    password_hash,
    preferred_payment_method,
    role,
    status
) VALUES
    (1, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, '2026-03-25 08:30:00', '+212600000001', 'Test Admin', 'admin.seed@rideflow.local', '$2a$10$k8ju6CNiD77NrkBdwyHgZuzYvG5GKYlMyHAoaqQyTFrMA67nmK.Pa', 'CREDIT_CARD', 'ADMIN', 'ACTIVE'),
    (2, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, '2026-03-25 08:35:00', '+212600000002', 'Amina Customer', 'amina.seed@rideflow.local', '$2a$10$k8ju6CNiD77NrkBdwyHgZuzYvG5GKYlMyHAoaqQyTFrMA67nmK.Pa', 'WALLET', 'CUSTOMER', 'ACTIVE'),
    (3, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, NULL, '+212600000003', 'Youssef Customer', 'youssef.seed@rideflow.local', '$2a$10$k8ju6CNiD77NrkBdwyHgZuzYvG5GKYlMyHAoaqQyTFrMA67nmK.Pa', 'DEBIT_CARD', 'CUSTOMER', 'ACTIVE'),
    (4, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, NULL, '+212600000004', 'Suspended Rider', 'suspended.seed@rideflow.local', '$2a$10$k8ju6CNiD77NrkBdwyHgZuzYvG5GKYlMyHAoaqQyTFrMA67nmK.Pa', 'CREDIT_CARD', 'CUSTOMER', 'SUSPENDED');

INSERT INTO scooters (
    id,
    created_at,
    updated_at,
    version,
    battery_percentage,
    kilometers_traveled,
    latitude,
    longitude,
    last_activity_at,
    public_code,
    model,
    address,
    maintenance_notes,
    status
) VALUES
    (1, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, 92, 145.50, 33.5731100, -7.5898430, '2026-03-25 08:25:00', 'RF-CAS-001', 'Segway Ninebot Max', 'Casablanca Marina', NULL, 'AVAILABLE'),
    (2, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, 64, 212.40, 33.5898860, -7.6038690, '2026-03-25 08:40:00', 'RF-CAS-002', 'Xiaomi Pro 2', 'Maarif Boulevard', 'Check rear brake next service', 'IN_USE'),
    (3, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, 38, 301.10, 33.5974820, -7.6187820, '2026-03-25 07:55:00', 'RF-CAS-003', 'Segway ES4', 'Anfa Park', NULL, 'AVAILABLE'),
    (4, '2026-03-25 08:00:00', '2026-03-25 08:00:00', 0, 12, 410.00, 33.6041200, -7.6321000, '2026-03-25 07:45:00', 'RF-CAS-004', 'NIU KQi3', 'Twin Center', 'Battery below unlock threshold', 'LOCKED');

INSERT INTO rentals (
    id,
    created_at,
    updated_at,
    version,
    user_id,
    scooter_id,
    start_time,
    end_time,
    status,
    battery_at_start,
    battery_at_end,
    distance_traveled,
    duration_minutes,
    unlock_fee_applied,
    rate_per_minute_applied,
    total_cost
) VALUES
    (1, '2026-03-25 08:10:00', '2026-03-25 08:28:00', 0, 2, 1, '2026-03-25 08:12:00', '2026-03-25 08:28:00', 'COMPLETED', 95, 92, 4.20, 16, 1.00, 0.20, 4.20),
    (2, '2026-03-25 08:32:00', '2026-03-25 08:40:00', 0, 3, 2, '2026-03-25 08:34:00', NULL, 'ACTIVE', 70, NULL, NULL, NULL, 1.00, 0.20, NULL),
    (3, '2026-03-25 08:20:00', '2026-03-25 08:22:00', 0, 2, 3, NULL, NULL, 'CANCELLED', 40, 40, 0.00, 0, 1.00, 0.20, 0.00);

INSERT INTO payments (
    id,
    created_at,
    updated_at,
    version,
    rental_id,
    user_id,
    type,
    amount,
    status,
    payment_method,
    transaction_reference,
    failure_reason
) VALUES
    (1, '2026-03-25 08:10:00', '2026-03-25 08:10:02', 0, 1, 2, 'UNLOCK_FEE', 1.00, 'SUCCEEDED', 'WALLET', 'TX-UNLOCK-0001', NULL),
    (2, '2026-03-25 08:28:00', '2026-03-25 08:28:02', 0, 1, 2, 'FINAL_PAYMENT', 3.20, 'SUCCEEDED', 'WALLET', 'TX-FINAL-0001', NULL),
    (3, '2026-03-25 08:32:00', '2026-03-25 08:32:02', 0, 2, 3, 'UNLOCK_FEE', 1.00, 'SUCCEEDED', 'DEBIT_CARD', 'TX-UNLOCK-0002', NULL),
    (4, '2026-03-25 08:20:00', '2026-03-25 08:22:00', 0, 3, 2, 'UNLOCK_FEE', 1.00, 'REFUNDED', 'WALLET', 'TX-UNLOCK-0003', 'Reservation cancelled before ride start');

INSERT INTO receipts (
    id,
    created_at,
    updated_at,
    version,
    receipt_code,
    rental_id,
    generated_at,
    unlock_fee_charged,
    usage_cost,
    total_cost
) VALUES
    (1, '2026-03-25 08:28:00', '2026-03-25 08:28:00', 0, 'RCT-0001', 1, '2026-03-25 08:28:00', 1.00, 3.20, 4.20);

INSERT INTO audit_logs (
    id,
    created_at,
    updated_at,
    version,
    actor_user_id,
    entity_id,
    ip_address,
    user_agent,
    action_type,
    actor_role,
    entity_type,
    payload,
    status
) VALUES
    (1, '2026-03-25 08:10:00', '2026-03-25 08:10:00', 0, 2, 1, '127.0.0.1', 'seed-script', 'RENTAL_UNLOCK', 'CUSTOMER', 'RENTAL', '{"rentalId":1,"scooterCode":"RF-CAS-001"}', 'SUCCESS'),
    (2, '2026-03-25 08:28:00', '2026-03-25 08:28:00', 0, 2, 1, '127.0.0.1', 'seed-script', 'PAYMENT_SUCCEEDED', 'CUSTOMER', 'PAYMENT', '{"paymentId":2,"transactionReference":"TX-FINAL-0001"}', 'SUCCESS'),
    (3, '2026-03-25 08:22:00', '2026-03-25 08:22:00', 0, 2, 3, '127.0.0.1', 'seed-script', 'PAYMENT_REFUNDED', 'CUSTOMER', 'PAYMENT', '{"paymentId":4,"reason":"Reservation cancelled before ride start"}', 'SUCCESS');
