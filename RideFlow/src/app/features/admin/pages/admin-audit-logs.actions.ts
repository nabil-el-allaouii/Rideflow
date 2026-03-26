import { createActionGroup, props } from '@ngrx/store';
import { AdminAuditLogFilters, AdminAuditLogsPageResponse } from './admin-audit-logs.models';

export const adminAuditLogsActions = createActionGroup({
  source: 'Admin Audit Logs',
  events: {
    'Load Requested': props<{ filters: AdminAuditLogFilters }>(),
    'Load Succeeded': props<{ response: AdminAuditLogsPageResponse; filters: AdminAuditLogFilters }>(),
    'Load Failed': props<{ error: string }>()
  }
});
