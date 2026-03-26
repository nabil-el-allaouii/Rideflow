import { createFeatureSelector, createSelector } from '@ngrx/store';
import { adminPaymentsFeatureKey } from './admin-payments.reducer';
import { AdminPaymentsState } from './admin-payments.state';

export const selectAdminPaymentsState = createFeatureSelector<AdminPaymentsState>(adminPaymentsFeatureKey);

export const selectAdminPayments = createSelector(selectAdminPaymentsState, (state) => state.payments);
export const selectAdminPaymentsFilters = createSelector(selectAdminPaymentsState, (state) => state.filters);
export const selectAdminPaymentsLoading = createSelector(selectAdminPaymentsState, (state) => state.loading);
export const selectAdminPaymentsError = createSelector(selectAdminPaymentsState, (state) => state.error);
export const selectAdminPaymentsPage = createSelector(selectAdminPaymentsState, (state) => state.page);
export const selectAdminPaymentsSize = createSelector(selectAdminPaymentsState, (state) => state.size);
export const selectAdminPaymentsTotalElements = createSelector(selectAdminPaymentsState, (state) => state.totalElements);
export const selectAdminPaymentsTotalPages = createSelector(selectAdminPaymentsState, (state) => state.totalPages);
