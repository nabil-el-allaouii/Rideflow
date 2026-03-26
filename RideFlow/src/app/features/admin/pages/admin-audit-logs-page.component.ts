import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { AdminAuditLogsFacade } from './admin-audit-logs.facade';
import {
  AdminAuditLog,
  AdminAuditLogFilters,
  AuditActionType,
  AuditEntityType,
  AuditLogStatus
} from './admin-audit-logs.models';

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
  selector: 'app-admin-audit-logs-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-audit-logs-page.component.html',
  styleUrl: './admin-audit-logs-page.component.css'
})
export class AdminAuditLogsPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly auditLogsFacade = inject(AdminAuditLogsFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly logs = toSignal(this.auditLogsFacade.logs$, { initialValue: [] as AdminAuditLog[] });
  readonly loading = toSignal(this.auditLogsFacade.loading$, { initialValue: false });
  readonly error = toSignal(this.auditLogsFacade.error$, { initialValue: null });
  readonly page = toSignal(this.auditLogsFacade.page$, { initialValue: 0 });
  readonly totalPages = toSignal(this.auditLogsFacade.totalPages$, { initialValue: 0 });
  readonly totalElements = toSignal(this.auditLogsFacade.totalElements$, { initialValue: 0 });

  readonly selectedLogId = signal<number | null>(null);
  readonly selectedLog = computed(() =>
    this.logs().find((log) => log.id === this.selectedLogId()) ?? null
  );

  readonly actionOptions: AuditActionType[] = [
    'LOGIN',
    'LOGOUT',
    'REGISTER',
    'SCOOTER_CREATE',
    'SCOOTER_UPDATE',
    'SCOOTER_STATUS_CHANGE',
    'SCOOTER_DELETE',
    'RENTAL_UNLOCK',
    'RENTAL_START',
    'RENTAL_CANCEL',
    'RENTAL_END',
    'RENTAL_FORCE_END',
    'PAYMENT_INITIATED',
    'PAYMENT_SUCCEEDED',
    'PAYMENT_FAILED',
    'PAYMENT_REFUNDED',
    'USER_STATUS_CHANGE'
  ];

  readonly entityOptions: AuditEntityType[] = [
    'USER',
    'SCOOTER',
    'RENTAL',
    'PAYMENT',
    'RECEIPT',
    'PRICING_CONFIG',
    'AUTH'
  ];

  readonly statusOptions: AuditLogStatus[] = ['SUCCESS', 'FAILED'];

  readonly filterForm = this.fb.nonNullable.group({
    query: [''],
    actionType: [''],
    entityType: [''],
    status: [''],
    fromDate: [''],
    toDate: ['']
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
    { label: 'User Management', icon: 'users', route: '/admin/users' },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals' },
    { label: 'Payments', icon: 'payments', route: '/admin/payments' },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing' },
    { label: 'Audit Logs', icon: 'audit', route: '/admin/audit-logs', active: true }
  ];

  readonly footerMenuItems: MenuItem[] = [
    { label: 'Sign Out', icon: 'logout' }
  ];

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

  readonly failedCount = computed(() =>
    this.logs().filter((log) => log.status === 'FAILED').length
  );
  readonly authEventsCount = computed(() =>
    this.logs().filter((log) => log.entityType === 'AUTH').length
  );
  readonly paymentEventsCount = computed(() =>
    this.logs().filter((log) => log.entityType === 'PAYMENT').length
  );

  constructor() {
    effect(() => {
      const logs = this.logs();
      const selectedLogId = this.selectedLogId();

      if (!logs.length) {
        this.selectedLogId.set(null);
        return;
      }

      if (selectedLogId === null || !logs.some((log) => log.id === selectedLogId)) {
        this.selectedLogId.set(logs[0].id);
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
      actionType: '',
      entityType: '',
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

  selectLog(log: AdminAuditLog): void {
    this.selectedLogId.set(log.id);
  }

  formatDateTime(value: string | null | undefined): string {
    return value ? new Date(value).toLocaleString() : 'N/A';
  }

  formatLabel(value: string): string {
    return value.toLowerCase().replace(/_/g, ' ');
  }

  actorLabel(log: AdminAuditLog): string {
    if (log.actorUserFullName) {
      return log.actorUserFullName;
    }

    if (log.actorUserEmail) {
      return log.actorUserEmail;
    }

    return log.actorRole.toLowerCase();
  }

  payloadPreview(value: string | null | undefined): string {
    if (!value) {
      return 'No payload';
    }

    return value.length > 140 ? `${value.slice(0, 140)}...` : value;
  }

  private loadPage(page: number): void {
    this.auditLogsFacade.load(this.buildFilters(page));
  }

  private buildFilters(page: number): AdminAuditLogFilters {
    const raw = this.filterForm.getRawValue();

    return {
      query: raw.query.trim() || null,
      actionType: (raw.actionType || null) as AuditActionType | null,
      entityType: (raw.entityType || null) as AuditEntityType | null,
      status: (raw.status || null) as AuditLogStatus | null,
      fromDate: raw.fromDate ? new Date(raw.fromDate).toISOString() : null,
      toDate: raw.toDate ? new Date(`${raw.toDate}T23:59:59`).toISOString() : null,
      page,
      size: 25
    };
  }
}
