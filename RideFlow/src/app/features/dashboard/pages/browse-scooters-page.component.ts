import { Component, OnDestroy, OnInit, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  BrowseScooterCardComponent,
  BrowseScooterItem,
  BrowseScooterStatus
} from '../components/browse-scooter-card/browse-scooter-card.component';
import {
  ScooterMapComponent,
  ScooterMapMarker
} from '../components/scooter-map/scooter-map.component';
import { Router } from '@angular/router';
import { ScooterFacade } from './scooter.facade';
import { Scooter } from './scooter.models';
import { AuthFacade } from '../../auth/store/auth.facade';
import { PaymentFacade } from './payment.facade';
import { RentalFacade } from './rental.facade';
import { ActiveRental } from './rental.models';

type MenuIcon = 'dashboard' | 'scooter' | 'history' | 'profile' | 'admin' | 'logout';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

@Component({
  selector: 'app-browse-scooters-page',
  imports: [ReactiveFormsModule, BrowseScooterCardComponent, ScooterMapComponent],
  templateUrl: './browse-scooters-page.component.html',
  styleUrl: './browse-scooters-page.component.css'
})
export class BrowseScootersPageComponent implements OnInit, OnDestroy {
  private static readonly ACTIVE_RENTAL_SYNC_INTERVAL_MS = 5000;
  private static readonly RESERVATION_TIMEOUT_MS = 5 * 60 * 1000;

  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly scooterFacade = inject(ScooterFacade);
  private readonly authFacade = inject(AuthFacade);
  private readonly rentalFacade = inject(RentalFacade);
  private readonly paymentFacade = inject(PaymentFacade);

  readonly customerLatitude = 33.589886;
  readonly customerLongitude = -7.603869;
  readonly now = signal(Date.now());

  readonly filterForm = this.fb.nonNullable.group({
    query: [''],
    minBattery: [15],
    radiusKm: [2]
  });

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly availableScooters = toSignal(this.scooterFacade.availableScooters$, { initialValue: [] });
  readonly scooterLoading = toSignal(this.scooterFacade.availableLoading$, { initialValue: false });
  readonly scooterError = toSignal(this.scooterFacade.availableError$, { initialValue: null });
  readonly activeRental = toSignal(this.rentalFacade.activeRental$, { initialValue: null });
  readonly activeRentalLoading = toSignal(this.rentalFacade.activeLoading$, { initialValue: false });
  readonly rentalMutationInProgress = toSignal(this.rentalFacade.mutationInProgress$, {
    initialValue: false
  });
  readonly rentalMutationError = toSignal(this.rentalFacade.mutationError$, { initialValue: null });
  private readonly lastRentalMutation = toSignal(this.rentalFacade.lastMutation$, { initialValue: null });
  readonly paymentInProgress = toSignal(this.paymentFacade.processing$, { initialValue: false });
  readonly paymentError = toSignal(this.paymentFacade.error$, { initialValue: null });
  readonly paymentContext = toSignal(this.paymentFacade.mutationContext$, { initialValue: null });
  readonly lastProcessedPayment = toSignal(this.paymentFacade.lastProcessedPayment$, { initialValue: null });
  readonly lastFailedFinalRentalId = toSignal(this.paymentFacade.lastFailedRentalId$, { initialValue: null });
  private readonly lastPaymentMutation = toSignal(this.paymentFacade.lastMutation$, { initialValue: null });

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

  readonly reservationSecondsRemaining = computed(() => {
    const rental = this.activeRental();
    if (!rental || rental.status !== 'PENDING') {
      return null;
    }

    const expiresAt =
      new Date(rental.createdAt).getTime() + BrowseScootersPageComponent.RESERVATION_TIMEOUT_MS;
    return Math.max(0, Math.ceil((expiresAt - this.now()) / 1000));
  });

  readonly rideElapsedSeconds = computed(() => {
    const rental = this.activeRental();
    if (!rental || rental.status !== 'ACTIVE' || !rental.startTime) {
      return null;
    }

    return Math.max(0, Math.floor((this.now() - new Date(rental.startTime).getTime()) / 1000));
  });

  readonly rideTimerLabel = computed(() => this.formatSeconds(this.rideElapsedSeconds()));
  readonly reservationTimerLabel = computed(() => this.formatSeconds(this.reservationSecondsRemaining()));
  readonly preferredPaymentMethod = computed(() => this.authUser()?.paymentMethod ?? null);
  readonly preferredPaymentMethodLabel = computed(() =>
    this.formatPaymentMethod(this.preferredPaymentMethod())
  );
  readonly paymentMethodConfigured = computed(() => this.preferredPaymentMethod() !== null);

  readonly scooters = computed<BrowseScooterItem[]>(() =>
    this.availableScooters().map((scooter) => this.toBrowseScooter(scooter, this.activeRental()))
  );

  readonly availableMapMarkers = computed<ScooterMapMarker[]>(() =>
    this.availableScooters()
      .filter((scooter) => scooter.latitude !== null && scooter.longitude !== null)
      .map((scooter) => ({
        code: scooter.publicCode,
        name: scooter.model,
        distanceLabel: this.formatDistance(scooter.distanceKm),
        battery: scooter.batteryPercentage,
        longitude: scooter.longitude as number,
        latitude: scooter.latitude as number,
        area: scooter.address || 'RideFlow service zone'
      }))
  );

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Browse Scooters', icon: 'scooter', route: '/scooters', active: true },
    { label: 'Rental History', icon: 'history', route: '/history' },
    { label: 'My Profile', icon: 'profile', route: '/profile' }
  ];

  readonly footerMenuItems: MenuItem[] = [
    { label: 'Sign Out', icon: 'logout' }
  ];

  private timerHandle: ReturnType<typeof setInterval> | null = null;
  private lastHandledMutationAt = 0;
  private lastHandledPaymentMutationAt = 0;
  private lastHandledEndedRentalMutationAt = 0;
  private lastActiveRentalSyncAt = 0;
  private lastExpiredReservationRentalId: number | null = null;

  constructor() {
    effect(() => {
      const mutation = this.lastRentalMutation();
      if (!mutation || mutation.occurredAt <= this.lastHandledMutationAt) {
        return;
      }

      this.lastHandledMutationAt = mutation.occurredAt;
      this.loadScooters();

      if (mutation.type === 'end' && mutation.occurredAt > this.lastHandledEndedRentalMutationAt) {
        this.lastHandledEndedRentalMutationAt = mutation.occurredAt;

        if (this.paymentMethodConfigured()) {
          this.paymentFacade.processFinal({
            rentalId: mutation.rentalId
          });
        }
      }
    });

    effect(() => {
      const mutation = this.lastPaymentMutation();
      if (!mutation || mutation.occurredAt <= this.lastHandledPaymentMutationAt) {
        return;
      }

      this.lastHandledPaymentMutationAt = mutation.occurredAt;
      this.loadScooters();
    });

    effect(() => {
      const rental = this.activeRental();
      const secondsRemaining = this.reservationSecondsRemaining();

      if (!rental || rental.status !== 'PENDING') {
        this.lastExpiredReservationRentalId = null;
        return;
      }

      if (secondsRemaining === 0 && this.lastExpiredReservationRentalId !== rental.rentalId) {
        this.lastExpiredReservationRentalId = rental.rentalId;
        this.syncRentalState();
      }
    });
  }

  ngOnInit(): void {
    this.loadScooters();
    this.rentalFacade.loadActive();

    this.timerHandle = setInterval(() => {
      this.now.set(Date.now());
      this.syncOpenRentalPeriodically();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerHandle) {
      clearInterval(this.timerHandle);
    }
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
    this.loadScooters();
  }

  clearFilters(): void {
    this.filterForm.setValue({
      query: '',
      minBattery: 15,
      radiusKm: 2
    });
    this.loadScooters();
  }

  unlockScooter(scooterId: number): void {
    if (!this.paymentMethodConfigured()) {
      return;
    }

    this.paymentFacade.clearStatus();
    this.rentalFacade.clearMutationError();
    this.paymentFacade.processUnlock({
      scooterId
    });
  }

  startRide(): void {
    const rental = this.activeRental();
    if (!rental || rental.status !== 'PENDING') {
      return;
    }

    this.rentalFacade.clearMutationError();
    this.rentalFacade.startRide(rental.rentalId);
  }

  cancelRide(): void {
    const rental = this.activeRental();
    if (!rental || rental.status !== 'PENDING') {
      return;
    }

    this.paymentFacade.clearStatus();
    this.rentalFacade.clearMutationError();
    this.rentalFacade.cancelRide(rental.rentalId);
  }

  endRide(): void {
    const rental = this.activeRental();
    if (!rental || rental.status !== 'ACTIVE') {
      return;
    }

    this.paymentFacade.clearStatus();
    this.rentalFacade.clearMutationError();
    this.rentalFacade.endRide(rental.rentalId);
  }

  retryFinalPayment(): void {
    const rentalId = this.lastFailedFinalRentalId();
    if (!rentalId || !this.paymentMethodConfigured()) {
      return;
    }

    this.paymentFacade.processFinal({
      rentalId
    });
  }

  private loadScooters(): void {
    const filters = this.filterForm.getRawValue();

    this.scooterFacade.loadAvailable({
      query: filters.query.trim() || undefined,
      minBattery: filters.minBattery,
      radiusKm: filters.radiusKm,
      latitude: this.customerLatitude,
      longitude: this.customerLongitude,
      size: 50
    });
  }

  private syncOpenRentalPeriodically(): void {
    const rental = this.activeRental();
    if (!rental) {
      return;
    }

    const currentTime = Date.now();
    if (currentTime - this.lastActiveRentalSyncAt < BrowseScootersPageComponent.ACTIVE_RENTAL_SYNC_INTERVAL_MS) {
      return;
    }

    this.syncRentalState();
  }

  private syncRentalState(): void {
    this.lastActiveRentalSyncAt = Date.now();
    this.rentalFacade.loadActive();
    this.loadScooters();
  }

  private toBrowseScooter(scooter: Scooter, activeRental: ActiveRental | null): BrowseScooterItem {
    const isCurrentRentalScooter = activeRental?.scooterId === scooter.id;
    const hasOpenRental = activeRental !== null;

    let unlockLabel = scooter.unlockable ? 'Unlock & Ride' : (scooter.unlockBlockedReason || 'Unavailable');
    let unlockDisabled = !scooter.unlockable;

    if (hasOpenRental && !isCurrentRentalScooter) {
      unlockLabel = 'Finish current ride first';
      unlockDisabled = true;
    }

    if (isCurrentRentalScooter && activeRental?.status === 'PENDING') {
      unlockLabel = 'Reserved by you';
      unlockDisabled = true;
    }

    if (isCurrentRentalScooter && activeRental?.status === 'ACTIVE') {
      unlockLabel = 'Ride in progress';
      unlockDisabled = true;
    }

    if (!this.paymentMethodConfigured() && !isCurrentRentalScooter) {
      unlockLabel = 'Set payment method in profile';
      unlockDisabled = true;
    }

    return {
      id: scooter.id,
      name: scooter.model,
      code: scooter.publicCode,
      speed: 25,
      battery: scooter.batteryPercentage,
      distanceLabel: this.formatDistance(scooter.distanceKm),
      locationLabel: scooter.address || this.formatCoordinateLabel(scooter),
      pricePerMinute: '$0.15',
      status: this.resolveCardStatus(scooter, activeRental),
      unlockLabel,
      unlockDisabled
    };
  }

  private resolveCardStatus(scooter: Scooter, activeRental: ActiveRental | null): BrowseScooterStatus {
    if (activeRental?.scooterId === scooter.id) {
      return 'in-use';
    }

    if (scooter.status === 'MAINTENANCE' || scooter.status === 'DISABLED' || scooter.status === 'LOCKED') {
      return 'maintenance';
    }

    if (scooter.status === 'IN_USE' || scooter.status === 'RESERVED') {
      return 'in-use';
    }

    if (scooter.batteryPercentage < 20) {
      return 'low-battery';
    }

    return 'available';
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

  private formatCoordinateLabel(scooter: Scooter): string {
    if (scooter.latitude === null || scooter.longitude === null) {
      return 'Location unavailable';
    }

    return `${scooter.latitude.toFixed(4)}, ${scooter.longitude.toFixed(4)}`;
  }

  private formatSeconds(value: number | null): string {
    if (value === null) {
      return '--:--';
    }

    const minutes = Math.floor(value / 60);
    const seconds = value % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  private formatPaymentMethod(value: string | null | undefined): string {
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
