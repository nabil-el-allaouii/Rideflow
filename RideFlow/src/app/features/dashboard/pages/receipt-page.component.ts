import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { ReceiptApiService } from './receipt-api.service';
import { ReceiptFacade } from './receipt.facade';

type MenuIcon = 'dashboard' | 'scooter' | 'history' | 'profile' | 'admin' | 'logout' | 'users' | 'rentals';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-receipt-page',
  imports: [],
  templateUrl: './receipt-page.component.html',
  styleUrl: './receipt-page.component.css'
})
export class ReceiptPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authFacade = inject(AuthFacade);
  private readonly receiptFacade = inject(ReceiptFacade);
  private readonly receiptApi = inject(ReceiptApiService);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly receipt = toSignal(this.receiptFacade.currentReceipt$, { initialValue: null });
  readonly loading = toSignal(this.receiptFacade.loading$, { initialValue: false });
  readonly error = toSignal(this.receiptFacade.error$, { initialValue: null });

  readonly isAdmin = computed(() => this.authUser()?.role === 'ADMIN');
  readonly initials = computed(() => {
    const fullName = this.authUser()?.fullName?.trim();
    if (!fullName) {
      return 'RF';
    }

    return fullName
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  });

  readonly mainMenuItems = computed<MenuItem[]>(() =>
    this.isAdmin()
      ? [
          { label: 'Dashboard', icon: 'admin', route: '/admin/dashboard' },
          { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals', active: true },
          { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
          { label: 'User Management', icon: 'users', route: '/admin/users' }
        ]
      : [
          { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
          { label: 'Browse Scooters', icon: 'scooter', route: '/scooters' },
          { label: 'Rental History', icon: 'history', route: '/history', active: true },
          { label: 'My Profile', icon: 'profile', route: '/profile' }
        ]
  );

  readonly footerMenuItems = computed<MenuItem[]>(() =>
    [{ label: 'Sign Out', icon: 'logout' }]
  );

  ngOnInit(): void {
    const rentalId = Number(this.route.snapshot.paramMap.get('rentalId'));
    if (!Number.isFinite(rentalId) || rentalId <= 0) {
      void this.router.navigate([this.isAdmin() ? '/admin/rentals' : '/history']);
      return;
    }

    this.receiptFacade.load(rentalId);
  }

  ngOnDestroy(): void {
    this.receiptFacade.clear();
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

  downloadPdf(): void {
    const receipt = this.receipt();
    if (!receipt) {
      return;
    }

    this.receiptApi.downloadPdf(receipt.rentalId).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${receipt.receiptCode}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }

  goBack(): void {
    void this.router.navigate([this.isAdmin() ? '/admin/rentals' : '/history']);
  }

  formatDateTime(value: string | null | undefined): string {
    if (!value) {
      return 'N/A';
    }

    return new Date(value).toLocaleString();
  }

  formatCurrency(value: number): string {
    return `$${value.toFixed(2)}`;
  }
}
