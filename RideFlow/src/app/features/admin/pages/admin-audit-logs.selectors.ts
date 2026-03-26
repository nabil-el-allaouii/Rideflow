import { createFeatureSelector, createSelector } from '@ngrx/store';
import { adminAuditLogsFeatureKey } from './admin-audit-logs.reducer';
import { AdminAuditLogsState } from './admin-audit-logs.state';

export const selectAdminAuditLogsState = createFeatureSelector<AdminAuditLogsState>(adminAuditLogsFeatureKey);

export const selectAdminAuditLogs = createSelector(selectAdminAuditLogsState, (state) => state.logs);
export const selectAdminAuditLogsFilters = createSelector(selectAdminAuditLogsState, (state) => state.filters);
export const selectAdminAuditLogsLoading = createSelector(selectAdminAuditLogsState, (state) => state.loading);
export const selectAdminAuditLogsError = createSelector(selectAdminAuditLogsState, (state) => state.error);
export const selectAdminAuditLogsPage = createSelector(selectAdminAuditLogsState, (state) => state.page);
export const selectAdminAuditLogsSize = createSelector(selectAdminAuditLogsState, (state) => state.size);
export const selectAdminAuditLogsTotalElements = createSelector(selectAdminAuditLogsState, (state) => state.totalElements);
export const selectAdminAuditLogsTotalPages = createSelector(selectAdminAuditLogsState, (state) => state.totalPages);
