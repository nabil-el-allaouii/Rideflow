import { createReducer, on } from '@ngrx/store';
import { adminPaymentsActions } from './admin-payments.actions';
import { AdminPaymentsState, initialAdminPaymentsState } from './admin-payments.state';

export const adminPaymentsFeatureKey = 'adminPayments';

export const adminPaymentsReducer = createReducer<AdminPaymentsState>(
  initialAdminPaymentsState,
  on(adminPaymentsActions.loadRequested, (state, { filters }) => ({
    ...state,
    loading: true,
    error: null,
    filters
  })),
  on(adminPaymentsActions.loadSucceeded, (state, { response, filters }) => ({
    ...state,
    payments: response.content,
    loading: false,
    error: null,
    filters,
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages
  })),
  on(adminPaymentsActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  }))
);
