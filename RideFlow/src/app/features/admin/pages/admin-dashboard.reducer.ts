import { createReducer, on } from '@ngrx/store';
import { adminDashboardActions } from './admin-dashboard.actions';
import { AdminDashboardState, initialAdminDashboardState } from './admin-dashboard.state';

export const adminDashboardFeatureKey = 'adminDashboard';

export const adminDashboardReducer = createReducer<AdminDashboardState>(
  initialAdminDashboardState,
  on(adminDashboardActions.loadRequested, (state, { range }) => ({
    ...state,
    range,
    loading: true,
    error: null
  })),
  on(adminDashboardActions.loadSucceeded, (state, { statistics, rentalsReport, revenueReport, range }) => ({
    ...state,
    statistics,
    rentalsReport,
    revenueReport,
    range,
    loading: false,
    error: null
  })),
  on(adminDashboardActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  }))
);
