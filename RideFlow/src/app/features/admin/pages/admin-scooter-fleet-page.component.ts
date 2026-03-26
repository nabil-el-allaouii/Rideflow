import { Component, ElementRef, OnDestroy, OnInit, ViewChild, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import { AdminLocationPickerComponent, AdminLocationSelection } from '../components/admin-location-picker/admin-location-picker.component';
import { ScooterFacade } from '../../dashboard/pages/scooter.facade';
import { ScooterStatus } from '../../dashboard/pages/scooter.models';
import { ScooterMutationEvent } from '../../dashboard/pages/scooter.state';

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
  selector: 'app-admin-scooter-fleet-page',
  imports: [ReactiveFormsModule, AdminLocationPickerComponent],
  templateUrl: './admin-scooter-fleet-page.component.html',
  styleUrl: './admin-scooter-fleet-page.component.css'
})
export class AdminScooterFleetPageComponent implements OnInit, OnDestroy {
  private static readonly REFRESH_INTERVAL_MS = 5000;

  @ViewChild('fleetFormSection') private fleetFormSection?: ElementRef<HTMLElement>;
  @ViewChild('publicCodeInput') private publicCodeInput?: ElementRef<HTMLInputElement>;

  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authFacade = inject(AuthFacade);
  private readonly scooterFacade = inject(ScooterFacade);

  readonly scooters = toSignal(this.scooterFacade.adminScooters$, { initialValue: [] });
  readonly loading = toSignal(this.scooterFacade.adminLoading$, { initialValue: false });
  readonly saving = toSignal(this.scooterFacade.mutationInProgress$, { initialValue: false });
  readonly adminError = toSignal(this.scooterFacade.adminError$, { initialValue: null });
  private readonly mutationError = toSignal(this.scooterFacade.mutationError$, { initialValue: null });
  private readonly lastMutation = toSignal(this.scooterFacade.lastMutation$, { initialValue: null });

  readonly scooterStatuses: ScooterStatus[] = [
    'AVAILABLE',
    'RESERVED',
    'IN_USE',
    'LOCKED',
    'MAINTENANCE',
    'DISABLED'
  ];

  readonly searchForm = this.fb.nonNullable.group({
    query: [''],
    status: [''],
    minBattery: [0]
  });

  readonly scooterForm = this.fb.nonNullable.group({
    publicCode: ['', [Validators.required, Validators.maxLength(30)]],
    model: ['', [Validators.required, Validators.maxLength(100)]],
    batteryPercentage: [100, [Validators.required, Validators.min(0), Validators.max(100)]],
    latitude: [''],
    longitude: [''],
    address: ['', [Validators.maxLength(500)]],
    kilometersTraveled: ['0'],
    maintenanceNotes: ['', [Validators.maxLength(2000)]]
  });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet', active: true },
    { label: 'User Management', icon: 'users', route: '/admin/users' },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals' },
    { label: 'Payments', icon: 'payments', route: '/admin/payments' },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing' },
    { label: 'Audit Logs', icon: 'audit', route: '/admin/audit-logs' }
  ];

  readonly footerMenuItems: MenuItem[] = [
    { label: 'Sign Out', icon: 'logout' }
  ];

  showForm = false;
  error: string | null = null;
  formError: string | null = null;
  editingScooterId: number | null = null;
  private lastHandledMutationAt = 0;
  private refreshHandle: ReturnType<typeof setInterval> | null = null;
  private readonly visibilityChangeHandler = () => {
    if (document.visibilityState === 'visible') {
      this.loadScooters();
    }
  };

  constructor() {
    effect(() => {
      const loadError = this.adminError();
      if (loadError) {
        this.error = loadError;
      }
    });

    effect(() => {
      const mutationError = this.mutationError();

      if (!mutationError) {
        this.formError = null;
        return;
      }

      if (this.showForm) {
        this.formError = mutationError;
        return;
      }

      this.error = mutationError;
    });

    effect(() => {
      const mutation = this.lastMutation();
      if (!mutation || mutation.occurredAt <= this.lastHandledMutationAt) {
        return;
      }

      this.lastHandledMutationAt = mutation.occurredAt;
      this.handleMutationSuccess(mutation);
    });
  }

  get isEditing(): boolean {
    return this.editingScooterId !== null;
  }

  ngOnInit(): void {
    this.loadScooters();
    this.refreshHandle = setInterval(() => {
      if (document.visibilityState === 'visible') {
        this.loadScooters();
      }
    }, AdminScooterFleetPageComponent.REFRESH_INTERVAL_MS);
    document.addEventListener('visibilitychange', this.visibilityChangeHandler);
    window.addEventListener('focus', this.visibilityChangeHandler);
  }

  ngOnDestroy(): void {
    if (this.refreshHandle) {
      clearInterval(this.refreshHandle);
    }
    document.removeEventListener('visibilitychange', this.visibilityChangeHandler);
    window.removeEventListener('focus', this.visibilityChangeHandler);
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
    this.loadScooters();
  }

  clearSearch(): void {
    this.searchForm.setValue({
      query: '',
      status: '',
      minBattery: 0
    });
    this.loadScooters();
  }

  editScooter(scooter: any): void {
    this.showForm = true;
    this.formError = null;
    this.editingScooterId = scooter.id;
    this.scooterForm.setValue({
      publicCode: scooter.publicCode,
      model: scooter.model,
      batteryPercentage: scooter.batteryPercentage,
      latitude: scooter.latitude === null ? '' : `${scooter.latitude}`,
      longitude: scooter.longitude === null ? '' : `${scooter.longitude}`,
      address: scooter.address || '',
      kilometersTraveled: scooter.kilometersTraveled === null ? '' : `${scooter.kilometersTraveled}`,
      maintenanceNotes: scooter.maintenanceNotes || ''
    });
    this.scooterForm.controls.publicCode.disable();
    this.scrollToForm();
  }

  resetForm(): void {
    this.editingScooterId = null;
    this.formError = null;
    this.scooterForm.reset({
      publicCode: '',
      model: '',
      batteryPercentage: 100,
      latitude: '',
      longitude: '',
      address: '',
      kilometersTraveled: '0',
      maintenanceNotes: ''
    });
    this.scooterForm.controls.publicCode.enable();
  }

  prepareCreateForm(): void {
    this.resetForm();
    this.showForm = true;
    this.scrollToForm();
  }

  cancelForm(): void {
    this.resetForm();
    this.showForm = false;
  }

  saveScooter(): void {
    if (this.scooterForm.invalid) {
      this.formError = 'Fill in the required scooter fields before saving.';
      this.scooterForm.markAllAsTouched();
      return;
    }

    this.formError = null;
    this.error = null;
    this.scooterFacade.clearMutationError();

    const payload = this.buildPayload();

    if (this.isEditing) {
      this.scooterFacade.update(this.editingScooterId as number, {
        model: payload.model,
        batteryPercentage: payload.batteryPercentage,
        latitude: payload.latitude,
        longitude: payload.longitude,
        address: payload.address,
        kilometersTraveled: payload.kilometersTraveled,
        maintenanceNotes: payload.maintenanceNotes
      });
      return;
    }

    this.scooterFacade.create(payload);
  }

  updateStatus(scooter: any, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as ScooterStatus;
    if (!status || status === scooter.status) {
      return;
    }

    this.error = null;
    this.scooterFacade.clearMutationError();
    this.scooterFacade.updateStatus(scooter.id, status);
  }

  deleteScooter(scooter: any): void {
    const confirmed = window.confirm(`Delete scooter ${scooter.publicCode}?`);
    if (!confirmed) {
      return;
    }

    this.error = null;
    this.scooterFacade.clearMutationError();
    this.scooterFacade.delete(scooter.id);
  }

  getStatusLabel(status: ScooterStatus): string {
    return status.replace('_', ' ').toLowerCase();
  }

  getStatusClass(status: ScooterStatus, battery: number): string {
    if (status === 'AVAILABLE' && battery < 20) {
      return 'low-battery';
    }

    return status.toLowerCase().replace('_', '-');
  }

  formatLocation(scooter: any): string {
    if (scooter.address) {
      return scooter.address;
    }

    if (scooter.latitude !== null && scooter.longitude !== null) {
      return `${scooter.latitude.toFixed(4)}, ${scooter.longitude.toFixed(4)}`;
    }

    return 'No location';
  }

  formatTimestamp(value: string | null): string {
    if (!value) {
      return 'No activity';
    }

    return new Date(value).toLocaleString();
  }

  onLocationSelected(location: AdminLocationSelection): void {
    this.scooterForm.patchValue({
      latitude: `${location.latitude}`,
      longitude: `${location.longitude}`
    });
  }

  getSelectedLatitude(): number | null {
    return this.parseOptionalNumber(this.scooterForm.getRawValue().latitude);
  }

  getSelectedLongitude(): number | null {
    return this.parseOptionalNumber(this.scooterForm.getRawValue().longitude);
  }

  private loadScooters(): void {
    const search = this.searchForm.getRawValue();

    this.scooterFacade.loadAdmin({
      query: search.query.trim() || undefined,
      status: (search.status || null) as ScooterStatus | null,
      minBattery: search.minBattery,
      size: 100
    });
  }

  private handleMutationSuccess(mutation: ScooterMutationEvent): void {
    this.formError = null;
    this.error = null;

    if (mutation.type === 'create') {
      this.searchForm.setValue({
        query: '',
        status: '',
        minBattery: 0
      });
      this.showForm = false;
      this.resetForm();
      return;
    }

    if (mutation.type === 'update') {
      this.showForm = false;
      this.resetForm();
    }
  }

  private buildPayload() {
    const rawValue = this.scooterForm.getRawValue();

    return {
      publicCode: rawValue.publicCode.trim(),
      model: rawValue.model.trim(),
      batteryPercentage: Number(rawValue.batteryPercentage),
      latitude: this.parseOptionalNumber(rawValue.latitude),
      longitude: this.parseOptionalNumber(rawValue.longitude),
      address: rawValue.address.trim() || null,
      kilometersTraveled: this.parseOptionalNumber(rawValue.kilometersTraveled),
      maintenanceNotes: rawValue.maintenanceNotes.trim() || null
    };
  }

  private scrollToForm(): void {
    this.fleetFormSection?.nativeElement.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });

    setTimeout(() => {
      this.publicCodeInput?.nativeElement.focus();
    }, 150);
  }

  private parseOptionalNumber(value: string | number | null): number | null {
    if (value === null || value === undefined) {
      return null;
    }

    if (typeof value === 'number') {
      return Number.isNaN(value) ? null : value;
    }

    const trimmedValue = value.trim();
    if (trimmedValue === '') {
      return null;
    }

    const parsedValue = Number(trimmedValue);
    return Number.isNaN(parsedValue) ? null : parsedValue;
  }
}

