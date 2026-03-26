import { Component, computed, inject, OnInit } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { RentalStatus } from '../../dashboard/pages/rental.models';
import { AdminRentalsFacade } from './admin-rentals.facade';

type MenuIcon =
  | 'dashboard'
  | 'scooter'
  | 'users'
  | 'rentals'
  | 'payments'
  | 'pricing'
  | 'audit'
  | 'customer'
  | 'logout';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-admin-rentals-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-rentals-page.component.html',
  styleUrl: './admin-rentals-page.component.css'
})
export class AdminRentalsPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly adminRentalsFacade = inject(AdminRentalsFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly rentals = toSignal(this.adminRentalsFacade.rentals$, { initialValue: [] });
  readonly loading = toSignal(this.adminRentalsFacade.loading$, { initialValue: false });
  readonly exporting = toSignal(this.adminRentalsFacade.exporting$, { initialValue: false });
  readonly error = toSignal(this.adminRentalsFacade.error$, { initialValue: null });
  readonly page = toSignal(this.adminRentalsFacade.page$, { initialValue: 0 });
  readonly totalPages = toSignal(this.adminRentalsFacade.totalPages$, { initialValue: 0 });
  readonly totalElements = toSignal(this.adminRentalsFacade.totalElements$, { initialValue: 0 });
  readonly statuses: RentalStatus[] = ['PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'FORCE_ENDED'];

  readonly filterForm = this.fb.nonNullable.group({
    query: [''],
    status: [''],
    fromDate: [''],
    toDate: ['']
  });

  readonly initials = computed(() => {
    const fullName = this.authUser()?.fullName?.trim();
    if (!fullName) {
      return 'AD';
    }

    return fullName
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
    { label: 'User Management', icon: 'users', route: '/admin/users' },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals', active: true },
    { label: 'Payments', icon: 'payments', route: '/admin/payments' },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing' },
    { label: 'Audit Logs', icon: 'audit', route: '/admin/audit-logs' }
  ];

  readonly footerMenuItems: MenuItem[] = [
    { label: 'Sign Out', icon: 'logout' }
  ];

  ngOnInit(): void {
    this.loadRentals();
  }

  onMainMenuClick(item: MenuItem): void {
    if (item.route) {
      void this.router.navigate([item.route]);
    }
  }

  onFooterMenuClick(item: MenuItem): void {
    if (item.icon === 'logout') {
      this.authFacade.logout();
      return;
    }

    if (item.route) {
      void this.router.navigate([item.route]);
    }
  }

  applyFilters(): void {
    this.loadPage(0);
  }

  resetFilters(): void {
    this.filterForm.setValue({
      query: '',
      status: '',
      fromDate: '',
      toDate: ''
    });
    this.loadPage(0);
  }

  goToPreviousPage(): void {
    const currentPage = this.page();
    if (currentPage <= 0 || this.loading()) {
      return;
    }
    this.loadPage(currentPage - 1);
  }

  goToNextPage(): void {
    const currentPage = this.page();
    if (currentPage + 1 >= this.totalPages() || this.loading()) {
      return;
    }
    this.loadPage(currentPage + 1);
  }

  exportCsv(): void {
    this.adminRentalsFacade.exportCsv(this.buildFilters(this.page()));
  }

  openReceipt(rentalId: number): void {
    void this.router.navigate(['/admin/rentals', rentalId, 'receipt']);
  }

  formatDateTime(value: string | null): string {
    return value ? new Date(value).toLocaleString() : 'N/A';
  }

  formatAmount(value: number | null): string {
    return value === null ? '$0.00' : `$${value.toFixed(2)}`;
  }

  private loadRentals(): void {
    this.loadPage(0);
  }

  private loadPage(page: number): void {
    this.adminRentalsFacade.load(this.buildFilters(page));
  }

  private buildFilters(page: number) {
    const raw = this.filterForm.getRawValue();
    return {
      query: raw.query.trim() || null,
      status: (raw.status || null) as RentalStatus | null,
      fromDate: raw.fromDate ? new Date(raw.fromDate).toISOString() : null,
      toDate: raw.toDate ? new Date(`${raw.toDate}T23:59:59`).toISOString() : null,
      page,
      size: 25
    };
  }
}
