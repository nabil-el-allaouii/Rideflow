# Testing Strategy

## Scope

This project uses four testing layers:

1. Unit tests for service-layer business logic with JUnit 5 and Mockito.
2. Integration tests for HTTP endpoints and persistence behavior with Spring Boot Test, MockMvc, and H2.
3. Automated browser end-to-end tests with Playwright against a dedicated E2E frontend/backend environment.
4. Manual verification checklists for full rental flows, admin workflows, and frontend/browser coverage.

## Automated Backend Testing

### Tooling

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- H2 in-memory database under the `test` profile
- JaCoCo for coverage reporting

### Commands

Run the full backend suite:

```powershell
./mvnw test
```

Generate and inspect service-layer coverage:

```powershell
./mvnw test
powershell -ExecutionPolicy Bypass -File .\scripts\report-service-coverage.ps1
```

JaCoCo HTML report:

`target/site/jacoco/index.html`

### Current Result

Measured on `2026-03-25` after the latest full test run:

- Backend tests: `49`
- Failures: `0`
- Errors: `0`
- Service-layer instruction coverage for `com.rideflow.demo.service.impl`: `72.69%`

This exceeds the project target of `70%+` for the service layer.

## Unit Test Coverage

Unit tests currently cover:

- pricing defaults and update validation
- admin dashboard aggregation logic
- authentication registration, logout, login guard paths, refresh-token flow
- payment processing rules and validation
- rental lifecycle rules
- scooter creation, update, filtering, and validation
- user profile normalization and uniqueness checks
- admin user state transitions
- receipt generation and access control
- audit log writing and request metadata handling

Main test classes:

- [PricingServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\PricingServiceImplTest.java)
- [AdminDashboardServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\AdminDashboardServiceImplTest.java)
- [AuthServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\AuthServiceImplTest.java)
- [PaymentServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\PaymentServiceImplTest.java)
- [RentalServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\RentalServiceImplTest.java)
- [ScooterServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\ScooterServiceImplTest.java)
- [UserProfileServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\UserProfileServiceImplTest.java)
- [AdminUserServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\AdminUserServiceImplTest.java)
- [ReceiptServiceImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\ReceiptServiceImplTest.java)
- [AuditLogWriterImplTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\service\impl\AuditLogWriterImplTest.java)

## Integration Test Coverage

Integration tests run against H2 with Spring MVC and security enabled.

Covered endpoint groups:

- pricing
- admin statistics
- admin users
- admin audit logs
- current user profile
- profile update
- receipt JSON/HTML/PDF endpoints

Main integration classes:

- [AdminApiIntegrationTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\api\controller\AdminApiIntegrationTest.java)
- [CustomerApiIntegrationTest.java](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\java\com\rideflow\demo\api\controller\CustomerApiIntegrationTest.java)

Test-specific config:

- [application-test.properties](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\resources\application-test.properties)

## Automated End-to-End Testing

Frontend/browser E2E tests are implemented with Playwright in the Angular app:

- [playwright.config.ts](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow\playwright.config.ts)
- [customer-rental-flow.spec.ts](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow\e2e\customer-rental-flow.spec.ts)
- [admin-operations.spec.ts](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow\e2e\admin-operations.spec.ts)

### E2E Environment

The Playwright run starts dedicated local servers automatically:

- Angular frontend on `http://127.0.0.1:4300`
- Spring Boot backend on `http://127.0.0.1:18080`
- Spring Boot `e2e` profile backed by seeded H2 data

E2E-specific backend config:

- [application-e2e.properties](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\main\resources\application-e2e.properties)

### Commands

Install the Playwright browser once:

```powershell
cd ..\RideFlow
npm run e2e:install
```

Run the full browser E2E suite:

```powershell
cd ..\RideFlow
npm run e2e
```

### Covered Flows

Current Playwright coverage includes:

- customer registration
- profile payment-method configuration
- scooter browse flow
- unlock payment flow
- cancel pending reservation
- start ride
- end ride
- rental history
- receipt view
- admin login
- admin dashboard access
- pricing update
- scooter creation
- user management access
- payments monitor access
- rental monitor access

### Current Result

Measured on `2026-03-25` after the latest full Playwright run:

- Playwright specs: `2`
- Passed: `2`
- Failed: `0`

This means the project now has actual automated browser E2E coverage, not only documented manual scenarios.

## Seed Data

Reusable sample data is provided in:

- [test-data.sql](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\src\test\resources\seed\test-data.sql)

It includes:

- one active pricing config
- one admin user
- three customer users
- four scooters in mixed states
- completed, active, and cancelled rentals
- unlock and final payments
- a sample receipt
- audit-log rows for reporting screens

Seeded password for seeded users:

- `Password123!`

Suggested usage for local MySQL verification:

```sql
SOURCE src/test/resources/seed/test-data.sql;
```

If your SQL client does not support `SOURCE`, run the file contents directly after clearing the target schema.

## Manual End-to-End Checklist

The following flows should still be executed manually after loading seed data or creating fresh test data, especially for visual validation, scheduler timing, and browser-specific behavior that is outside the current Playwright coverage.

### Customer flows

1. Register a new user and log in.
2. Configure preferred payment method in profile.
3. Browse scooters and verify filters.
4. Unlock a scooter and confirm unlock-fee payment succeeds.
5. Cancel a pending reservation and confirm refund status.
6. Unlock again, start a ride, end the ride, and verify:
   - rental status
   - scooter status
   - battery decrement
   - pricing calculation
   - receipt generation
7. Open rental history and receipt details.
8. Update profile email/phone/name and verify authenticated navigation still works.

### Admin flows

1. Log in as admin.
2. Manage scooters:
   - create
   - update
   - delete
   - status change
3. Open rental monitor and verify pagination and filters.
4. Open payments monitor and verify listing, metrics, and details.
5. Open pricing page and update active pricing.
6. Open user management and change a customer status.
7. Open audit logs and verify customer activity appears.
8. Open admin dashboard and confirm metrics/charts reflect live data.

### Edge cases

1. Attempt unlock with battery below threshold.
2. Attempt unlock with no payment method configured.
3. Attempt duplicate ride end request.
4. Let reservation timeout expire.
5. Retry a failed final payment.
6. Verify cancelled reservation marks unlock payment as refunded.

## Frontend Cross-Browser and Responsive Testing

Automated browser testing is now present through Playwright on Chromium. The checklist below remains required for manual validation across additional browsers and responsive layouts.

Browsers to verify:

- Chrome latest
- Edge latest
- Firefox latest

Viewport widths to verify:

- `390px` mobile
- `768px` tablet
- `1280px` desktop
- `1440px+` wide desktop

Screens to verify:

- auth login/register
- customer dashboard
- browse scooters / map
- profile
- rental history / receipt
- admin dashboard
- admin fleet
- admin users
- admin rentals
- admin payments
- admin pricing
- admin audit logs

Responsive checks:

- sidebar behavior
- table overflow
- form button accessibility
- map rendering
- receipt readability
- pagination controls

## Known Limits and Follow-Up

- Browser compatibility and responsive checks across Chrome, Edge, and Firefox still need explicit manual execution.
- The automated suite now includes backend tests plus Chromium-based Playwright flows, but Angular unit/component tests are not implemented yet.
- Scheduler-driven behaviors such as reservation timeout still benefit from explicit manual verification because they depend on elapsed wall-clock time.
- Payment success/failure is simulated; it is not a real gateway integration.

## Recommendation

Keep `./mvnw test` as the minimum backend gate and use the coverage script before merging service-layer changes that touch pricing, rentals, payments, auth, or audit behavior.
