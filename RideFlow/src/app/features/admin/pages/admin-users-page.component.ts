import { Component, computed, effect, inject, OnInit } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { UserRole, UserStatus } from '../../auth/store/auth.models';
import { AdminUsersFacade } from './admin-users.facade';
import { AdminUser } from './admin-users.models';

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
  selector: 'app-admin-users-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-users-page.component.html',
  styleUrl: './admin-users-page.component.css'
})
export class AdminUsersPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly adminUsersFacade = inject(AdminUsersFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly users = toSignal(this.adminUsersFacade.users$, { initialValue: [] });
  readonly selectedUser = toSignal(this.adminUsersFacade.selectedUser$, { initialValue: null });
  readonly loading = toSignal(this.adminUsersFacade.listLoading$, { initialValue: false });
  readonly detailLoading = toSignal(this.adminUsersFacade.detailLoading$, { initialValue: false });
  readonly mutationInProgress = toSignal(this.adminUsersFacade.mutationInProgress$, {
    initialValue: false
  });
  readonly error = toSignal(this.adminUsersFacade.error$, { initialValue: null });
  readonly mutationError = toSignal(this.adminUsersFacade.mutationError$, { initialValue: null });
  readonly totalElements = toSignal(this.adminUsersFacade.totalElements$, { initialValue: 0 });

  readonly roleOptions: UserRole[] = ['CUSTOMER', 'ADMIN'];
  readonly statusOptions: UserStatus[] = ['ACTIVE', 'SUSPENDED', 'DISABLED'];

  readonly searchForm = this.fb.nonNullable.group({
    query: [''],
    role: [''],
    status: ['']
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
    { label: 'User Management', icon: 'users', route: '/admin/users', active: true },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals' },
    { label: 'Payments', icon: 'payments', route: '/admin/payments' },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing' },
    { label: 'Audit Logs', icon: 'audit', route: '/admin/audit-logs' }
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

  readonly activeUsersCount = computed(
    () => this.users().filter((user) => user.status === 'ACTIVE').length
  );
  readonly adminUsersCount = computed(
    () => this.users().filter((user) => user.role === 'ADMIN').length
  );
  readonly suspendedUsersCount = computed(
    () => this.users().filter((user) => user.status === 'SUSPENDED').length
  );

  constructor() {
    effect(() => {
      const users = this.users();
      const selectedUser = this.selectedUser();
      const detailLoading = this.detailLoading();

      if (!users.length || detailLoading) {
        return;
      }

      if (!selectedUser || !users.some((user) => user.id === selectedUser.id)) {
        this.adminUsersFacade.loadDetail(users[0].id);
      }
    });
  }

  ngOnInit(): void {
    this.loadUsers();
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

  applySearch(): void {
    this.loadUsers();
  }

  clearSearch(): void {
    this.searchForm.setValue({
      query: '',
      role: '',
      status: ''
    });
    this.loadUsers();
  }

  selectUser(user: AdminUser): void {
    this.adminUsersFacade.loadDetail(user.id);
  }

  updateUserStatus(user: AdminUser, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as UserStatus;
    if (!status || status === user.status) {
      return;
    }

    this.adminUsersFacade.clearMutationError();
    this.adminUsersFacade.updateStatus(user.id, status);
  }

  updateSelectedUserStatus(event: Event): void {
    const user = this.selectedUser();
    if (!user) {
      return;
    }

    this.updateUserStatus(user, event);
  }

  getStatusLabel(status: UserStatus): string {
    return status.toLowerCase();
  }

  getRoleLabel(role: UserRole): string {
    return role.toLowerCase();
  }

  formatDateTime(value: string | null | undefined): string {
    if (!value) {
      return 'No activity';
    }

    return new Date(value).toLocaleString();
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'Unknown';
    }

    return new Date(value).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  private loadUsers(): void {
    const value = this.searchForm.getRawValue();

    this.adminUsersFacade.loadList({
      query: value.query.trim() || undefined,
      role: (value.role || undefined) as UserRole | undefined,
      status: (value.status || undefined) as UserStatus | undefined,
      page: 0,
      size: 50
    });
  }
}
