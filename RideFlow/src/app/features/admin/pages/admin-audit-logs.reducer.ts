import { createReducer, on } from '@ngrx/store';
import { adminAuditLogsActions } from './admin-audit-logs.actions';
import { AdminAuditLogsState, initialAdminAuditLogsState } from './admin-audit-logs.state';

export const adminAuditLogsFeatureKey = 'adminAuditLogs';

export const adminAuditLogsReducer = createReducer<AdminAuditLogsState>(
  initialAdminAuditLogsState,
  on(adminAuditLogsActions.loadRequested, (state, { filters }) => ({
    ...state,
    loading: true,
    error: null,
    filters
  })),
  on(adminAuditLogsActions.loadSucceeded, (state, { response, filters }) => ({
    ...state,
    logs: response.content,
    loading: false,
    error: null,
    filters,
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages
  })),
  on(adminAuditLogsActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  }))
);
