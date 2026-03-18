import { Component, OnInit, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  HistoryItemComponent,
  RentalHistoryRecord
} from '../components/history-item/history-item.component';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { RentalFacade } from './rental.facade';
import { Rental, RentalStatus } from './rental.models';

type MenuIcon = 'dashboard' | 'scooter' | 'history' | 'profile' | 'admin' | 'logout';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-rental-history-page',
  imports: [ReactiveFormsModule, HistoryItemComponent],
  templateUrl: './rental-history-page.component.html',
  styleUrl: './rental-history-page.component.css'
})
export class RentalHistoryPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly rentalFacade = inject(RentalFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly history = toSignal(this.rentalFacade.history$, { initialValue: [] });
  readonly isLoading = toSignal(this.rentalFacade.historyLoading$, { initialValue: false });
  readonly error = toSignal(this.rentalFacade.error$, { initialValue: null });
  readonly statuses: RentalStatus[] = ['COMPLETED', 'CANCELLED', 'FORCE_ENDED'];

  readonly filterForm = this.fb.nonNullable.group({
    query: [''],
    status: [''],
    fromDate: [''],
    toDate: ['']
  });

  readonly records = computed<RentalHistoryRecord[]>(() =>
    this.history().map((rental) => this.toHistoryRecord(rental))
  );

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

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Browse Scooters', icon: 'scooter', route: '/scooters' },
    { label: 'Rental History', icon: 'history', route: '/history', active: true },
    { label: 'My Profile', icon: 'profile', route: '/profile' }
  ];

  readonly footerMenuItems: MenuItem[] = [
    { label: 'Sign Out', icon: 'logout' }
  ];

  ngOnInit(): void {
    this.loadHistory();
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
    this.loadHistory();
  }

  resetFilters(): void {
    this.filterForm.setValue({
      query: '',
      status: '',
      fromDate: '',
      toDate: ''
    });
    this.loadHistory();
  }

  openReceipt(rentalId: number): void {
    void this.router.navigate(['/history', rentalId, 'receipt']);
  }

  private loadHistory(): void {
    const raw = this.filterForm.getRawValue();
    this.rentalFacade.loadHistory({
      query: raw.query.trim() || null,
      status: (raw.status || null) as RentalStatus | null,
      fromDate: raw.fromDate ? new Date(raw.fromDate).toISOString() : null,
      toDate: raw.toDate ? new Date(`${raw.toDate}T23:59:59`).toISOString() : null,
      page: 0,
      size: 50
    });
  }

  private toHistoryRecord(rental: Rental): RentalHistoryRecord {
    return {
      rentalId: rental.id,
      scooterName: rental.scooterModel,
      rentalCode: rental.scooterCode,
      dateLabel: this.formatDate(rental.createdAt),
      durationLabel: rental.durationMinutes ? `${rental.durationMinutes} min` : 'Pending',
      distanceLabel: rental.distanceTraveled !== null ? `${rental.distanceTraveled.toFixed(1)} km` : 'N/A',
      amountLabel: rental.totalCost !== null ? rental.totalCost.toFixed(2) : rental.unlockFeeApplied.toFixed(2),
      receiptAvailable: rental.receiptAvailable,
      status:
        rental.status === 'COMPLETED'
          ? 'completed'
          : rental.status === 'FORCE_ENDED'
            ? 'force-ended'
            : 'cancelled'
    };
  }

  private formatDate(value: string): string {
    return new Date(value).toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }
}
