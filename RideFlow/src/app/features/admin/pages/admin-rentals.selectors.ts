import { createFeatureSelector, createSelector } from '@ngrx/store';
import { adminRentalsFeatureKey } from './admin-rentals.reducer';
import { AdminRentalsState } from './admin-rentals.state';

export const selectAdminRentalsState = createFeatureSelector<AdminRentalsState>(adminRentalsFeatureKey);

export const selectAdminRentals = createSelector(selectAdminRentalsState, (state) => state.rentals);
export const selectAdminRentalsFilters = createSelector(selectAdminRentalsState, (state) => state.filters);
export const selectAdminRentalsLoading = createSelector(selectAdminRentalsState, (state) => state.loading);
export const selectAdminRentalsExporting = createSelector(selectAdminRentalsState, (state) => state.exporting);
export const selectAdminRentalsError = createSelector(selectAdminRentalsState, (state) => state.error);
export const selectAdminRentalsPage = createSelector(selectAdminRentalsState, (state) => state.page);
export const selectAdminRentalsSize = createSelector(selectAdminRentalsState, (state) => state.size);
export const selectAdminRentalsTotalElements = createSelector(selectAdminRentalsState, (state) => state.totalElements);
export const selectAdminRentalsTotalPages = createSelector(selectAdminRentalsState, (state) => state.totalPages);
