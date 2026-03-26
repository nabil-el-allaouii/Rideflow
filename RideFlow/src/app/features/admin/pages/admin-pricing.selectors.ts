import { createFeatureSelector, createSelector } from '@ngrx/store';
import { adminPricingFeatureKey } from './admin-pricing.reducer';
import { AdminPricingState } from './admin-pricing.state';

export const selectAdminPricingState = createFeatureSelector<AdminPricingState>(adminPricingFeatureKey);

export const selectAdminPricing = createSelector(selectAdminPricingState, (state) => state.pricing);
export const selectAdminPricingLoading = createSelector(selectAdminPricingState, (state) => state.loading);
export const selectAdminPricingSaving = createSelector(selectAdminPricingState, (state) => state.saving);
export const selectAdminPricingError = createSelector(selectAdminPricingState, (state) => state.error);
export const selectAdminPricingSuccessMessage = createSelector(selectAdminPricingState, (state) => state.successMessage);
