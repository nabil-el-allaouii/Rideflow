import { createReducer, on } from '@ngrx/store';
import { adminPricingActions } from './admin-pricing.actions';
import { AdminPricingState, initialAdminPricingState } from './admin-pricing.state';

export const adminPricingFeatureKey = 'adminPricing';

export const adminPricingReducer = createReducer<AdminPricingState>(
  initialAdminPricingState,
  on(adminPricingActions.loadRequested, (state) => ({
    ...state,
    loading: true,
    error: null
  })),
  on(adminPricingActions.loadSucceeded, (state, { pricing }) => ({
    ...state,
    pricing,
    loading: false,
    error: null
  })),
  on(adminPricingActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(adminPricingActions.updateRequested, (state) => ({
    ...state,
    saving: true,
    error: null,
    successMessage: null
  })),
  on(adminPricingActions.updateSucceeded, (state, { pricing }) => ({
    ...state,
    pricing,
    saving: false,
    error: null,
    successMessage: 'Pricing updated successfully.'
  })),
  on(adminPricingActions.updateFailed, (state, { error }) => ({
    ...state,
    saving: false,
    error
  })),
  on(adminPricingActions.clearFeedback, (state) => ({
    ...state,
    error: null,
    successMessage: null
  }))
);
