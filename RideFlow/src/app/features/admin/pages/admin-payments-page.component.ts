import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { Payment } from '../../dashboard/pages/payment.models';
import { PaymentStatus, PaymentType } from '../../dashboard/pages/rental.models';
import { AdminPaymentsFacade } from './admin-payments.facade';
import { AdminPaymentFilters } from './admin-payments.models';

type MenuIcon =
  | 'dashboard'
  | 'scooter'
  | 'users'
  | 'rentals'
  | 'payments'
  | 'pricing'
  | 'audit'
  | 'logout';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-admin-payments-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-payments-page.component.html',
  styleUrl: './admin-payments-page.component.css'
})
export class AdminPaymentsPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly adminPaymentsFacade = inject(AdminPaymentsFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly payments = toSignal(this.adminPaymentsFacade.payments$, { initialValue: [] as Payment[] });
  readonly loading = toSignal(this.adminPaymentsFacade.loading$, { initialValue: false });
  readonly error = toSignal(this.adminPaymentsFacade.error$, { initialValue: null });
  readonly page = toSignal(this.adminPaymentsFacade.page$, { initialValue: 0 });
  readonly totalPages = toSignal(this.adminPaymentsFacade.totalPages$, { initialValue: 0 });
  readonly totalElements = toSignal(this.adminPaymentsFacade.totalElements$, { initialValue: 0 });

  readonly selectedPaymentId = signal<number | null>(null);
  readonly selectedPayment = computed(() =>
    this.payments().find((payment) => payment.id === this.selectedPaymentId()) ?? null
  );

  readonly typeOptions: PaymentType[] = ['UNLOCK_FEE', 'FINAL_PAYMENT'];
  readonly statusOptions: PaymentStatus[] = ['PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'REFUNDED'];

  readonly filterForm = this.fb.nonNullable.group({
    query: [''],
    type: [''],
    status: [''],
    userId: [''],
    rentalId: [''],
    fromDate: [''],
    toDate: ['']
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
    { label: 'User Management', icon: 'users', route: '/admin/users' },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals' },
    { label: 'Payments', icon: 'payments', route: '/admin/payments', active: true },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing' },
    { label: 'Audit Logs', icon: 'audit', route: '/admin/audit-logs' }
  ];

  readonly footerMenuItems: MenuItem[] = [{ label: 'Sign Out', icon: 'logout' }];

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

  readonly successfulVolume = computed(() =>
    this.payments()
      .filter((payment) => payment.status === 'SUCCEEDED')
      .reduce((total, payment) => total + payment.amount, 0)
  );

  readonly failedCount = computed(() =>
    this.payments().filter((payment) => payment.status === 'FAILED').length
  );

  readonly refundedCount = computed(() =>
    this.payments().filter((payment) => payment.status === 'REFUNDED').length
  );

  constructor() {
    effect(() => {
      const payments = this.payments();
      const selectedPaymentId = this.selectedPaymentId();

      if (!payments.length) {
        this.selectedPaymentId.set(null);
        return;
      }

      if (selectedPaymentId === null || !payments.some((payment) => payment.id === selectedPaymentId)) {
        this.selectedPaymentId.set(payments[0].id);
      }
    });
  }

  ngOnInit(): void {
    this.loadPage(0);
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
      type: '',
      status: '',
      userId: '',
      rentalId: '',
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

  selectPayment(payment: Payment): void {
    this.selectedPaymentId.set(payment.id);
  }

  openReceipt(payment: Payment): void {
    void this.router.navigate(['/admin/rentals', payment.rentalId, 'receipt']);
  }

  formatDateTime(value: string | null | undefined): string {
    return value ? new Date(value).toLocaleString() : 'N/A';
  }

  formatAmount(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '$0.00';
    }
    return `$${value.toFixed(2)}`;
  }

  formatLabel(value: string | null | undefined): string {
    if (!value) {
      return 'N/A';
    }
    return value.toLowerCase().replace(/_/g, ' ');
  }

  private loadPage(page: number): void {
    this.adminPaymentsFacade.load(this.buildFilters(page));
  }

  private buildFilters(page: number): AdminPaymentFilters {
    const raw = this.filterForm.getRawValue();

    return {
      query: raw.query.trim() || null,
      type: (raw.type || null) as PaymentType | null,
      status: (raw.status || null) as PaymentStatus | null,
      userId: this.parseOptionalNumber(raw.userId),
      rentalId: this.parseOptionalNumber(raw.rentalId),
      fromDate: raw.fromDate ? new Date(raw.fromDate).toISOString() : null,
      toDate: raw.toDate ? new Date(`${raw.toDate}T23:59:59`).toISOString() : null,
      page,
      size: 25
    };
  }

  private parseOptionalNumber(value: string): number | null {
    const normalized = value.trim();
    if (!normalized) {
      return null;
    }

    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
