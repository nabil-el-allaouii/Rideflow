import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { adminPricingActions } from './admin-pricing.actions';
import { PricingConfigRequest } from './admin-pricing.models';
import {
  selectAdminPricing,
  selectAdminPricingError,
  selectAdminPricingLoading,
  selectAdminPricingSaving,
  selectAdminPricingSuccessMessage
} from './admin-pricing.selectors';

@Injectable({ providedIn: 'root' })
export class AdminPricingFacade {
  private readonly store = inject(Store);

  readonly pricing$ = this.store.select(selectAdminPricing);
  readonly loading$ = this.store.select(selectAdminPricingLoading);
  readonly saving$ = this.store.select(selectAdminPricingSaving);
  readonly error$ = this.store.select(selectAdminPricingError);
  readonly successMessage$ = this.store.select(selectAdminPricingSuccessMessage);

  load(): void {
    this.store.dispatch(adminPricingActions.loadRequested());
  }

  update(payload: PricingConfigRequest): void {
    this.store.dispatch(adminPricingActions.updateRequested({ payload }));
  }

  clearFeedback(): void {
    this.store.dispatch(adminPricingActions.clearFeedback());
  }
}
