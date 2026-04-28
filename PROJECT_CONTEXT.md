# RideFlow Project Context

## Overview
RideFlow is a **scooter rental platform** built as a monorepo with two main components:
- **Frontend**: Angular 21 application
- **Backend**: Spring Boot 4.0 (Java 17) REST API

## Directory Structure
```
Rideflow/
├── RideFlow/                    # Angular Frontend
│   ├── src/app/
│   │   ├── core/               # Config, guards, interceptors
│   │   ├── features/
│   │   │   ├── auth/           # Login, register, auth guards
│   │   │   ├── admin/          # Admin dashboard, fleet, users, payments, pricing, audit logs
│   │   │   └── dashboard/      # Customer dashboard, browse scooters, rental history, profile
│   │   ├── app.routes.ts       # Route definitions
│   │   └── app.config.ts       # App configuration (NgRx, interceptors)
│   ├── e2e/                    # Playwright e2e tests
│   └── package.json
├── RideFlow-Backend/            # Spring Boot Backend
│   ├── src/main/java/com/rideflow/demo/
│   │   ├── api/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/            # Request/Response DTOs
│   │   │   └── exception/      # Global exception handling
│   │   ├── config/             # Security, OpenAPI, bootstrap configs
│   │   ├── domain/
│   │   │   ├── model/          # JPA entities
│   │   │   ├── repository/     # Spring Data repositories
│   │   │   └── enums/          # Status enums
│   │   └── security/           # JWT filter, user details service
│   └── pom.xml
├── docker-compose.yml           # MySQL, Backend, Frontend services
└── DOCKER.md                    # Docker setup documentation
```

## Backend Stack
- **Framework**: Spring Boot 4.0.3
- **Java Version**: 17
- **Database**: MySQL 8.4
- **Authentication**: JWT (jjwt 0.12.7)
- **API Documentation**: SpringDoc OpenAPI 3.0.2
- **PDF Generation**: OpenPDF 2.0.5
- **Testing**: JUnit, Spring Security Test, H2 in-memory DB
- **Build**: Maven with Jacoco code coverage

## Frontend Stack
- **Framework**: Angular 21.1.4
- **State Management**: NgRx 21.0.1 (Store, Effects)
- **Styling**: Tailwind CSS 4.1.12
- **Maps**: Mapbox GL 3.18.1
- **Testing**: Vitest, Playwright
- **Package Manager**: npm 10.8.2

## Domain Models

### User
| Field | Type |
|-------|------|
| email | String (unique) |
| passwordHash | String |
| fullName | String |
| phoneNumber | String |
| preferredPaymentMethod | PaymentMethod enum |
| role | UserRole (ADMIN, CUSTOMER) |
| status | UserStatus (ACTIVE, SUSPENDED, INACTIVE) |
| lastLoginAt | Instant |

### Scooter
| Field | Type |
|-------|------|
| publicCode | String (unique) |
| model | String |
| batteryPercentage | Integer |
| latitude/longitude | BigDecimal |
| address | String |
| status | ScooterStatus (AVAILABLE, IN_USE, MAINTENANCE, RETIRED) |
| kilometersTraveled | BigDecimal |
| maintenanceNotes | String |
| lastActivityAt | Instant |

### Rental
| Field | Type |
|-------|------|
| user | ManyToOne User |
| scooter | ManyToOne Scooter |
| startTime/endTime | Instant |
| status | RentalStatus (ACTIVE, COMPLETED, CANCELLED) |
| batteryAtStart/batteryAtEnd | Integer |
| distanceTraveled | BigDecimal |
| durationMinutes | Integer |
| unlockFeeApplied | BigDecimal |
| ratePerMinuteApplied | BigDecimal |
| totalCost | BigDecimal |

### Supporting Models
- **Payment**: id, rental, user, amount, status, method, type, transactionId
- **Receipt**: id, rental, generatedAt, pdfData
- **PricingConfig**: unlockFee, ratePerMinute, currency
- **AuditLog**: actorUser, action, entityType, entityId, details, timestamp
- **RefreshToken**: user, token, expiresAt
- **PasswordResetToken**: user, token, expiresAt

## API Endpoints

### Public (no auth required)
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - Logout
- `POST /api/auth/password-reset/**` - Password reset
- `GET /api/scooters/available` - List available scooters (authenticated)
- Swagger UI: `/swagger-ui.html`

### Authenticated Users
- `GET/PUT /api/profile` - User profile
- `GET /api/scooters/available` - Browse available scooters
- `POST /api/rentals/unlock` - Unlock scooter to start rental
- `POST /api/rentals/{id}/end` - End rental
- `GET /api/rentals/active` - Get active rental
- `GET /api/rentals/history` - Rental history
- `POST /api/payments/initiate` - Initiate payment
- `GET /api/receipts/{id}` - Get receipt (PDF)

### Admin Only (`/api/admin/**`)
- `GET /api/admin/dashboard/stats` - Dashboard statistics
- `GET/POST/PUT/DELETE /api/admin/users` - User management
- `GET/POST/PUT/DELETE /api/admin/scooters` - Scooter management
- `GET/PUT /api/admin/rentals/{id}/force-end` - Force end rental
- `GET /api/admin/payments` - View all payments
- `GET/POST/PUT /api/admin/pricing` - Pricing configuration
- `GET /api/admin/audit-logs` - Audit log queries

## Frontend Routes

### Public
- `/auth/login` - Login page
- `/auth/register` - Registration page

### Customer (authenticated, role: CUSTOMER)
- `/dashboard` - Main customer dashboard
- `/scooters` - Browse and rent scooters
- `/history` - Rental history
- `/history/:rentalId/receipt` - View receipt
- `/profile` - User profile

### Admin (authenticated, role: ADMIN)
- `/admin/dashboard` - Admin dashboard with stats
- `/admin/fleet` - Scooter fleet management
- `/admin/users` - User management
- `/admin/rentals` - Rental management
- `/admin/payments` - Payment overview
- `/admin/pricing` - Pricing configuration
- `/admin/audit-logs` - Audit logs viewer

## NgRx State Structure
Each feature module (admin-users, admin-payments, admin-rentals, admin-pricing, admin-dashboard, admin-audit-logs) follows the NgRx pattern:
- `*.actions.ts` - Action definitions
- `*.reducer.ts` - State reducer
- `*.effects.ts` - Side effects (API calls)
- `*.selectors.ts` - State selectors
- `*.facade.ts` - Facade service
- `*.state.ts` - State interface
- `*.models.ts` - TypeScript interfaces

## Security
- JWT-based stateless authentication
- BCrypt password hashing
- Role-based access control (ADMIN, CUSTOMER)
- CORS configured for allowed origins
- Audit logging for admin actions

## Environment Variables (Backend)
| Variable | Description |
|----------|-------------|
| DB_URL | JDBC MySQL connection URL |
| DB_USERNAME | Database username |
| DB_PASSWORD | Database password |
| JWT_SECRET | JWT signing secret |
| APP_SECURITY_ALLOWED_ORIGINS | CORS allowed origins |
| BOOTSTRAP_ADMIN_EMAIL/PASSWORD | Initial admin account |

## Key Files Reference
- **Backend Entry**: `RideFlow-Backend/src/main/java/com/rideflow/demo/RideflowApplication.java`
- **Security Config**: `RideFlow-Backend/src/main/java/com/rideflow/demo/config/SecurityConfig.java`
- **Auth Controller**: `RideFlow-Backend/src/main/java/com/rideflow/demo/api/controller/AuthController.java`
- **Frontend App Config**: `RideFlow/src/app/app.config.ts`
- **Routes**: `RideFlow/src/app/app.routes.ts`
- **API Config**: `RideFlow/src/app/core/config/api.config.ts`

## Docker Deployment
- MySQL service on internal port 3306
- Backend on port 8080 (accessible to frontend)
- Frontend on port 4200 (nginx serving Angular app)
- Default admin: `admin@rideflow.local` / `Admin1234`

## Testing
- **Backend Unit Tests**: Spring Boot Test with H2 database
- **Backend Integration Tests**: `AdminApiIntegrationTest`, `CustomerApiIntegrationTest`
- **Frontend Unit Tests**: Vitest with Angular
- **E2E Tests**: Playwright (`e2e/` directory)

## Code Style
- **Prettier**: 100 character line width, single quotes
- **TypeScript**: Strict mode
- **Java**: Spring Boot conventions, JPA best practices