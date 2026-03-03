import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideMapboxGL } from 'ngx-mapbox-gl';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';

import { routes } from './app.routes';
import { MAPBOX_ACCESS_TOKEN_VALUE } from './core/config/mapbox.config';
import { authInterceptor } from './features/auth/data-access/auth.interceptor';
import { AuthEffects } from './features/auth/store/auth.effects';
import { authFeatureKey, authReducer } from './features/auth/store/auth.reducer';
import { AdminDashboardEffects } from './features/admin/pages/admin-dashboard.effects';
import { adminDashboardFeatureKey, adminDashboardReducer } from './features/admin/pages/admin-dashboard.reducer';
import { AdminUsersEffects } from './features/admin/pages/admin-users.effects';
import { adminUsersFeatureKey, adminUsersReducer } from './features/admin/pages/admin-users.reducer';
import { AdminRentalsEffects } from './features/admin/pages/admin-rentals.effects';
import { adminRentalsFeatureKey, adminRentalsReducer } from './features/admin/pages/admin-rentals.reducer';
import { AdminPaymentsEffects } from './features/admin/pages/admin-payments.effects';
import { adminPaymentsFeatureKey, adminPaymentsReducer } from './features/admin/pages/admin-payments.reducer';
import { AdminPricingEffects } from './features/admin/pages/admin-pricing.effects';
import { adminPricingFeatureKey, adminPricingReducer } from './features/admin/pages/admin-pricing.reducer';
import { AdminAuditLogsEffects } from './features/admin/pages/admin-audit-logs.effects';
import { adminAuditLogsFeatureKey, adminAuditLogsReducer } from './features/admin/pages/admin-audit-logs.reducer';
import { ProfileEffects } from './features/dashboard/pages/profile.effects';
import { profileFeatureKey, profileReducer } from './features/dashboard/pages/profile.reducer';
import { PaymentEffects } from './features/dashboard/pages/payment.effects';
import { paymentFeatureKey, paymentReducer } from './features/dashboard/pages/payment.reducer';
import { ReceiptEffects } from './features/dashboard/pages/receipt.effects';
import { receiptFeatureKey, receiptReducer } from './features/dashboard/pages/receipt.reducer';
import { RentalEffects } from './features/dashboard/pages/rental.effects';
import { rentalFeatureKey, rentalReducer } from './features/dashboard/pages/rental.reducer';
import { ScooterEffects } from './features/dashboard/pages/scooter.effects';
import { scooterFeatureKey, scooterReducer } from './features/dashboard/pages/scooter.reducer';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
    provideStore({
      [authFeatureKey]: authReducer,
      [scooterFeatureKey]: scooterReducer,
      [profileFeatureKey]: profileReducer,
      [adminDashboardFeatureKey]: adminDashboardReducer,
      [adminUsersFeatureKey]: adminUsersReducer,
      [adminRentalsFeatureKey]: adminRentalsReducer,
      [adminPaymentsFeatureKey]: adminPaymentsReducer,
      [adminPricingFeatureKey]: adminPricingReducer,
      [adminAuditLogsFeatureKey]: adminAuditLogsReducer,
      [rentalFeatureKey]: rentalReducer,
      [paymentFeatureKey]: paymentReducer,
      [receiptFeatureKey]: receiptReducer
    }),
    provideEffects([
      AuthEffects,
      ScooterEffects,
      ProfileEffects,
      AdminDashboardEffects,
      AdminUsersEffects,
      AdminRentalsEffects,
      AdminPaymentsEffects,
      AdminPricingEffects,
      AdminAuditLogsEffects,
      RentalEffects,
      PaymentEffects,
      ReceiptEffects
    ]),
    provideMapboxGL({ accessToken: MAPBOX_ACCESS_TOKEN_VALUE })
  ]
};
