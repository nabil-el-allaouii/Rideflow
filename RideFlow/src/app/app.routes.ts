import { Routes } from '@angular/router';
import { appEntryGuard } from './features/auth/data-access/app-entry.guard';
import { authGuard } from './features/auth/data-access/auth.guard';
import { guestGuard } from './features/auth/data-access/guest.guard';
import { roleGuard } from './features/auth/data-access/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [appEntryGuard],
    children: []
  },
  {
    path: 'auth/login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page.component').then(
        (m) => m.LoginPageComponent
      )
  },
  {
    path: 'auth/register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/pages/register-page/register-page.component').then(
        (m) => m.RegisterPageComponent
      )
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard-page.component').then(
        (m) => m.DashboardPageComponent
      )
  },
  {
    path: 'scooters',
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/dashboard/pages/browse-scooters-page.component').then(
        (m) => m.BrowseScootersPageComponent
      )
  },
  {
    path: 'history',
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/dashboard/pages/rental-history-page.component').then(
        (m) => m.RentalHistoryPageComponent
      )
  },
  {
    path: 'history/:rentalId/receipt',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/pages/receipt-page.component').then(
        (m) => m.ReceiptPageComponent
      )
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/pages/profile-page.component').then(
        (m) => m.ProfilePageComponent
      )
  },
  {
    path: 'admin/dashboard',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-dashboard-page.component').then(
        (m) => m.AdminDashboardPageComponent
      )
  },
  {
    path: 'admin/fleet',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-scooter-fleet-page.component').then(
        (m) => m.AdminScooterFleetPageComponent
      )
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-users-page.component').then(
        (m) => m.AdminUsersPageComponent
      )
  },
  {
    path: 'admin/rentals',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-rentals-page.component').then(
        (m) => m.AdminRentalsPageComponent
      )
  },
  {
    path: 'admin/payments',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-payments-page.component').then(
        (m) => m.AdminPaymentsPageComponent
      )
  },
  {
    path: 'admin/pricing',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-pricing-page.component').then(
        (m) => m.AdminPricingPageComponent
      )
  },
  {
    path: 'admin/audit-logs',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/pages/admin-audit-logs-page.component').then(
        (m) => m.AdminAuditLogsPageComponent
      )
  },
  {
    path: 'admin/rentals/:rentalId/receipt',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/dashboard/pages/receipt-page.component').then(
        (m) => m.ReceiptPageComponent
      )
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
