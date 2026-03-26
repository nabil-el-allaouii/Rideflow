import { createFeatureSelector, createSelector } from '@ngrx/store';
import { adminDashboardFeatureKey } from './admin-dashboard.reducer';
import { AdminDashboardState } from './admin-dashboard.state';

export const selectAdminDashboardState = createFeatureSelector<AdminDashboardState>(adminDashboardFeatureKey);

export const selectAdminDashboardStatistics = createSelector(selectAdminDashboardState, (state) => state.statistics);
export const selectAdminDashboardRentalsReport = createSelector(selectAdminDashboardState, (state) => state.rentalsReport);
export const selectAdminDashboardRevenueReport = createSelector(selectAdminDashboardState, (state) => state.revenueReport);
export const selectAdminDashboardRange = createSelector(selectAdminDashboardState, (state) => state.range);
export const selectAdminDashboardLoading = createSelector(selectAdminDashboardState, (state) => state.loading);
export const selectAdminDashboardError = createSelector(selectAdminDashboardState, (state) => state.error);
