import { Component, OnInit, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { StatCardComponent, StatMetric } from '../components/stat-card/stat-card.component';
import { ScooterCardComponent, ScooterItem } from '../components/scooter-card/scooter-card.component';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { RentalFacade } from './rental.facade';
import { ScooterFacade } from './scooter.facade';
import { defaultRentalHistoryFilters } from './rental.state';

type MenuIcon = 'dashboard' | 'scooter' | 'history' | 'profile' | 'admin' | 'logout';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [StatCardComponent, ScooterCardComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css'
})
export class DashboardPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authFacade = inject(AuthFacade);
  private readonly rentalFacade = inject(RentalFacade);
  private readonly scooterFacade = inject(ScooterFacade);

  private readonly customerLatitude = 33.589886;
  private readonly customerLongitude = -7.603869;

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly rentalHistory = toSignal(this.rentalFacade.history$, { initialValue: [] });
  readonly activeRental = toSignal(this.rentalFacade.activeRental$, { initialValue: null });
  readonly availableScooters = toSignal(this.scooterFacade.availableScooters$, { initialValue: [] });
  readonly rentalHistoryLoading = toSignal(this.rentalFacade.historyLoading$, { initialValue: false });
  readonly activeRentalLoading = toSignal(this.rentalFacade.activeLoading$, { initialValue: false });
  readonly scooterLoading = toSignal(this.scooterFacade.availableLoading$, { initialValue: false });

  readonly dashboardLoading = computed(
    () => this.rentalHistoryLoading() || this.activeRentalLoading() || this.scooterLoading()
  );

  readonly displayName = computed(() => {
    const fullName = this.authUser()?.fullName?.trim();
    if (!fullName) {
      return 'Rider';
    }

    return fullName.split(/\s+/)[0];
  });

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

  readonly paymentMethodConfigured = computed(() => !!this.authUser()?.paymentMethod);

  readonly metricCards = computed<StatMetric[]>(() => {
    const completedRides = this.rentalHistory().filter(
      (rental) => rental.status === 'COMPLETED' || rental.status === 'FORCE_ENDED'
    );
    const totalDistance = completedRides.reduce(
      (sum, rental) => sum + (rental.distanceTraveled ?? 0),
      0
    );
    const totalDurationMinutes = completedRides.reduce(
      (sum, rental) => sum + (rental.durationMinutes ?? 0),
      0
    );
    const averageDurationMinutes = completedRides.length === 0
      ? 0
      : Math.round(totalDurationMinutes / completedRides.length);

    return [
      {
        value: completedRides.length.toString(),
        label: 'Total Rides',
        helper: this.activeRental() ? '1 ride currently open' : 'No ride in progress',
        icon: 'rides'
      },
      {
        value: `${totalDistance.toFixed(totalDistance >= 10 ? 0 : 1)} km`,
        label: 'Distance',
        helper: completedRides.length === 0 ? 'No completed rides yet' : `${completedRides.length} completed rides`,
        icon: 'distance'
      },
      {
        value: this.formatDuration(totalDurationMinutes),
        label: 'Ride Time',
        helper: completedRides.length === 0 ? 'Average unavailable' : `Avg ${this.formatDuration(averageDurationMinutes)} / ride`,
        icon: 'time'
      }
    ];
  });

  readonly scooters = computed<ScooterItem[]>(() =>
    this.availableScooters()
      .slice(0, 4)
      .map((scooter) => ({
        name: scooter.model,
        code: scooter.publicCode,
        battery: scooter.batteryPercentage,
        distanceLabel: this.formatDistance(scooter.distanceKm),
        status: scooter.batteryPercentage < 20 ? 'low-battery' : 'available'
      }))
  );

  readonly latestRideSummary = computed(() => {
    const latestRide = [...this.rentalHistory()]
      .sort((left, right) => new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime())[0];

    if (!latestRide) {
      return 'No rides yet. Your first trip will appear here once you complete it.';
    }

    return `${latestRide.scooterCode} · ${latestRide.status.toLowerCase().replace('_', ' ')} · ${this.formatDuration(latestRide.durationMinutes ?? 0)}`;
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', active: true, route: '/dashboard' },
    { label: 'Browse Scooters', icon: 'scooter', route: '/scooters' },
    { label: 'Rental History', icon: 'history', route: '/history' },
    { label: 'My Profile', icon: 'profile', route: '/profile' }
  ];

  readonly footerMenuItems: MenuItem[] = [
    { label: 'Sign Out', icon: 'logout' }
  ];

  ngOnInit(): void {
    this.rentalFacade.loadHistory(defaultRentalHistoryFilters);
    this.rentalFacade.loadActive();
    this.scooterFacade.loadAvailable({
      minBattery: 15,
      latitude: this.customerLatitude,
      longitude: this.customerLongitude,
      radiusKm: 3,
      size: 8
    });
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

  openScooters(): void {
    void this.router.navigate(['/scooters']);
  }

  openProfile(): void {
    void this.router.navigate(['/profile']);
  }

  openHistory(): void {
    void this.router.navigate(['/history']);
  }

  private formatDistance(distanceKm: number | null): string {
    if (distanceKm === null) {
      return 'Distance N/A';
    }

    if (distanceKm < 1) {
      return `${Math.round(distanceKm * 1000)}m`;
    }

    return `${distanceKm.toFixed(1)}km`;
  }

  private formatDuration(minutes: number): string {
    if (minutes <= 0) {
      return '0 min';
    }

    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;

    if (hours === 0) {
      return `${remainingMinutes} min`;
    }

    if (remainingMinutes === 0) {
      return `${hours} hr`;
    }

    return `${hours} hr ${remainingMinutes} min`;
  }
}
