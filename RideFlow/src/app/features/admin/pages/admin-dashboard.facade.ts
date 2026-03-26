import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { adminDashboardActions } from './admin-dashboard.actions';
import { AdminDashboardLoadRange } from './admin-dashboard.models';
import {
  selectAdminDashboardError,
  selectAdminDashboardLoading,
  selectAdminDashboardRange,
  selectAdminDashboardRentalsReport,
  selectAdminDashboardRevenueReport,
  selectAdminDashboardStatistics
} from './admin-dashboard.selectors';

@Injectable({ providedIn: 'root' })
export class AdminDashboardFacade {
  private readonly store = inject(Store);

  readonly statistics$ = this.store.select(selectAdminDashboardStatistics);
  readonly rentalsReport$ = this.store.select(selectAdminDashboardRentalsReport);
  readonly revenueReport$ = this.store.select(selectAdminDashboardRevenueReport);
  readonly range$ = this.store.select(selectAdminDashboardRange);
  readonly loading$ = this.store.select(selectAdminDashboardLoading);
  readonly error$ = this.store.select(selectAdminDashboardError);

  load(range: AdminDashboardLoadRange): void {
    this.store.dispatch(adminDashboardActions.loadRequested({ range }));
  }
}
