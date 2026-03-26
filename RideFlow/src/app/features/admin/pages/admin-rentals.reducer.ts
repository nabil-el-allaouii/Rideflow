import { createReducer, on } from '@ngrx/store';
import { adminRentalsActions } from './admin-rentals.actions';
import { AdminRentalsState, initialAdminRentalsState } from './admin-rentals.state';

export const adminRentalsFeatureKey = 'adminRentals';

export const adminRentalsReducer = createReducer<AdminRentalsState>(
  initialAdminRentalsState,
  on(adminRentalsActions.loadRequested, (state, { filters }) => ({
    ...state,
    filters,
    loading: true,
    error: null
  })),
  on(adminRentalsActions.loadSucceeded, (state, { response, filters }) => ({
    ...state,
    rentals: response.content,
    filters,
    loading: false,
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages
  })),
  on(adminRentalsActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(adminRentalsActions.exportRequested, (state) => ({
    ...state,
    exporting: true,
    error: null
  })),
  on(adminRentalsActions.exportSucceeded, (state) => ({
    ...state,
    exporting: false
  })),
  on(adminRentalsActions.exportFailed, (state, { error }) => ({
    ...state,
    exporting: false,
    error
  }))
);
