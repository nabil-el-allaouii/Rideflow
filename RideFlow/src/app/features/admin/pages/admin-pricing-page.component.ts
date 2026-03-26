import { Component, computed, effect, inject, OnInit } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { AdminPricingFacade } from './admin-pricing.facade';

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
  selector: 'app-admin-pricing-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-pricing-page.component.html',
  styleUrl: './admin-pricing-page.component.css'
})
export class AdminPricingPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly pricingFacade = inject(AdminPricingFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly pricing = toSignal(this.pricingFacade.pricing$, { initialValue: null });
  readonly loading = toSignal(this.pricingFacade.loading$, { initialValue: false });
  readonly saving = toSignal(this.pricingFacade.saving$, { initialValue: false });
  readonly error = toSignal(this.pricingFacade.error$, { initialValue: null });
  readonly successMessage = toSignal(this.pricingFacade.successMessage$, { initialValue: null });

  readonly pricingForm = this.fb.nonNullable.group({
    unlockFee: [1, [Validators.required, Validators.min(0)]],
    ratePerMinute: [0.15, [Validators.required, Validators.min(0)]],
    batteryConsumptionRate: [0.5, [Validators.required, Validators.min(0.01)]],
    currency: ['USD', [Validators.required, Validators.maxLength(10)]]
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
    { label: 'User Management', icon: 'users', route: '/admin/users' },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals' },
    { label: 'Payments', icon: 'payments', route: '/admin/payments' },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing', active: true },
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

  readonly projectedTenMinuteCost = computed(() => {
    const rate = this.pricingForm.controls.ratePerMinute.value || 0;
    const unlock = this.pricingForm.controls.unlockFee.value || 0;
    return unlock + rate * 10;
  });

  readonly projectedTwentyMinuteBatteryUse = computed(() => {
    const rate = this.pricingForm.controls.batteryConsumptionRate.value || 0;
    return rate * 20;
  });

  constructor() {
    effect(() => {
      const pricing = this.pricing();
      if (!pricing) {
        return;
      }

      this.pricingForm.patchValue(
        {
          unlockFee: pricing.unlockFee,
          ratePerMinute: pricing.ratePerMinute,
          batteryConsumptionRate: pricing.batteryConsumptionRate,
          currency: pricing.currency
        },
        { emitEvent: false }
      );
    });
  }

  ngOnInit(): void {
    this.pricingFacade.load();
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

  submit(): void {
    this.pricingFacade.clearFeedback();
    if (this.pricingForm.invalid) {
      this.pricingForm.markAllAsTouched();
      return;
    }

    const value = this.pricingForm.getRawValue();
    this.pricingFacade.update({
      unlockFee: Number(value.unlockFee),
      ratePerMinute: Number(value.ratePerMinute),
      batteryConsumptionRate: Number(value.batteryConsumptionRate),
      currency: value.currency.trim().toUpperCase()
    });
  }

  reset(): void {
    const pricing = this.pricing();
    this.pricingFacade.clearFeedback();

    if (!pricing) {
      this.pricingForm.reset({
        unlockFee: 1,
        ratePerMinute: 0.15,
        batteryConsumptionRate: 0.5,
        currency: 'USD'
      });
      return;
    }

    this.pricingForm.reset({
      unlockFee: pricing.unlockFee,
      ratePerMinute: pricing.ratePerMinute,
      batteryConsumptionRate: pricing.batteryConsumptionRate,
      currency: pricing.currency
    });
  }

  formatCurrencyAmount(value: number | null | undefined): string {
    const amount = value ?? 0;
    const currency = this.pricingForm.controls.currency.value?.trim().toUpperCase() || this.pricing()?.currency || 'USD';
    return `${amount.toFixed(2)} ${currency}`;
  }

  formatDateTime(value: string | null | undefined): string {
    return value ? new Date(value).toLocaleString() : 'N/A';
  }
}
