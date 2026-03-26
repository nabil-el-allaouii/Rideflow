import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { adminPaymentsActions } from './admin-payments.actions';
import { AdminPaymentFilters } from './admin-payments.models';
import {
  selectAdminPayments,
  selectAdminPaymentsError,
  selectAdminPaymentsFilters,
  selectAdminPaymentsLoading,
  selectAdminPaymentsPage,
  selectAdminPaymentsSize,
  selectAdminPaymentsTotalElements,
  selectAdminPaymentsTotalPages
} from './admin-payments.selectors';

@Injectable({ providedIn: 'root' })
export class AdminPaymentsFacade {
  private readonly store = inject(Store);

  readonly payments$ = this.store.select(selectAdminPayments);
  readonly filters$ = this.store.select(selectAdminPaymentsFilters);
  readonly loading$ = this.store.select(selectAdminPaymentsLoading);
  readonly error$ = this.store.select(selectAdminPaymentsError);
  readonly page$ = this.store.select(selectAdminPaymentsPage);
  readonly size$ = this.store.select(selectAdminPaymentsSize);
  readonly totalElements$ = this.store.select(selectAdminPaymentsTotalElements);
  readonly totalPages$ = this.store.select(selectAdminPaymentsTotalPages);

  load(filters: AdminPaymentFilters): void {
    this.store.dispatch(adminPaymentsActions.loadRequested({ filters }));
  }
}
