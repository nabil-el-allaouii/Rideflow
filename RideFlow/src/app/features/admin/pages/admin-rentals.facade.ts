import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { adminRentalsActions } from './admin-rentals.actions';
import { AdminRentalFilters } from './admin-rentals.models';
import {
  selectAdminRentals,
  selectAdminRentalsError,
  selectAdminRentalsExporting,
  selectAdminRentalsFilters,
  selectAdminRentalsLoading,
  selectAdminRentalsPage,
  selectAdminRentalsSize,
  selectAdminRentalsTotalElements,
  selectAdminRentalsTotalPages
} from './admin-rentals.selectors';

@Injectable({ providedIn: 'root' })
export class AdminRentalsFacade {
  private readonly store = inject(Store);

  readonly rentals$ = this.store.select(selectAdminRentals);
  readonly filters$ = this.store.select(selectAdminRentalsFilters);
  readonly loading$ = this.store.select(selectAdminRentalsLoading);
  readonly exporting$ = this.store.select(selectAdminRentalsExporting);
  readonly error$ = this.store.select(selectAdminRentalsError);
  readonly page$ = this.store.select(selectAdminRentalsPage);
  readonly size$ = this.store.select(selectAdminRentalsSize);
  readonly totalElements$ = this.store.select(selectAdminRentalsTotalElements);
  readonly totalPages$ = this.store.select(selectAdminRentalsTotalPages);

  load(filters: AdminRentalFilters): void {
    this.store.dispatch(adminRentalsActions.loadRequested({ filters }));
  }

  exportCsv(filters: AdminRentalFilters): void {
    this.store.dispatch(adminRentalsActions.exportRequested({ filters }));
  }
}
