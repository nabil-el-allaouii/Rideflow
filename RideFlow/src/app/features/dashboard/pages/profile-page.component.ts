import { Component, computed, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { ProfileFacade } from './profile.facade';
import type { ProfilePaymentMethod } from './profile.models';

type MenuIcon = 'dashboard' | 'scooter' | 'history' | 'profile' | 'admin' | 'logout';
type UserRole = 'CUSTOMER' | 'ADMIN';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.css'
})
export class ProfilePageComponent {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly profileFacade = inject(ProfileFacade);

  readonly profileForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', [Validators.maxLength(30)]],
    paymentMethod: this.fb.nonNullable.control<ProfilePaymentMethod | ''>('')
  });

  readonly paymentMethods: Array<{ value: ProfilePaymentMethod; label: string }> = [
    { value: 'CREDIT_CARD', label: 'Credit Card' },
    { value: 'DEBIT_CARD', label: 'Debit Card' },
    { value: 'WALLET', label: 'Wallet' }
  ];

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly profile = toSignal(this.profileFacade.profile$, { initialValue: null });
  readonly loading = toSignal(this.profileFacade.loading$, { initialValue: false });
  readonly saving = toSignal(this.profileFacade.saving$, { initialValue: false });
  readonly error = toSignal(this.profileFacade.error$, { initialValue: null });
  readonly successMessage = toSignal(this.profileFacade.successMessage$, { initialValue: null });

  readonly userRole = computed<UserRole>(() => this.profile()?.role ?? this.authUser()?.role ?? 'CUSTOMER');

  readonly initials = computed(() => {
    const fullName = this.profile()?.fullName?.trim() ?? this.authUser()?.fullName?.trim();
    if (!fullName) {
      return 'RF';
    }

    return fullName
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  });

  readonly memberSince = computed(() => {
    const createdAt = this.profile()?.createdAt;
    return createdAt ? this.formatDate(createdAt) : 'Unknown';
  });

  readonly lastLogin = computed(() => {
    const lastLoginAt = this.profile()?.lastLoginAt;
    return lastLoginAt ? this.formatDateTime(lastLoginAt) : 'No login recorded';
  });

  readonly preferredPaymentMethodLabel = computed(() =>
    this.formatPaymentMethod(this.profile()?.paymentMethod ?? this.authUser()?.paymentMethod ?? null)
  );

  readonly mainMenuItems = computed<MenuItem[]>(() => {
    if (this.userRole() === 'ADMIN') {
      return [
        { label: 'Admin Dashboard', icon: 'admin', route: '/admin/dashboard' },
        { label: 'Fleet Management', icon: 'scooter', route: '/admin/fleet' },
        { label: 'My Profile', icon: 'profile', route: '/profile', active: true }
      ];
    }

    return [
      { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
      { label: 'Browse Scooters', icon: 'scooter', route: '/scooters' },
      { label: 'Rental History', icon: 'history', route: '/history' },
      { label: 'My Profile', icon: 'profile', route: '/profile', active: true }
    ];
  });

  readonly footerMenuItems = computed<MenuItem[]>(() => [
    { label: 'Sign Out', icon: 'logout' }
  ]);

  constructor() {
    effect(() => {
      const profile = this.profile();
      if (!profile) {
        return;
      }

      this.profileForm.patchValue(
        {
          fullName: profile.fullName,
          email: profile.email,
          phoneNumber: profile.phoneNumber ?? '',
          paymentMethod: profile.paymentMethod ?? ''
        },
        { emitEvent: false }
      );
    });

    this.profileFacade.load();
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

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.profileFacade.clearFeedback();
    const value = this.profileForm.getRawValue();
    this.profileFacade.update({
      fullName: value.fullName.trim(),
      email: value.email.trim(),
      phoneNumber: value.phoneNumber.trim() || null,
      paymentMethod: value.paymentMethod || null
    });
  }

  private formatDate(value: string): string {
    return new Date(value).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'long'
    });
  }

  private formatDateTime(value: string): string {
    return new Date(value).toLocaleString();
  }

  private formatPaymentMethod(value: ProfilePaymentMethod | null | undefined): string {
    if (!value) {
      return 'Not configured';
    }

    return value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
