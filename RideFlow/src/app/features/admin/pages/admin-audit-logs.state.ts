import { AdminAuditLog, AdminAuditLogFilters } from './admin-audit-logs.models';

export interface AdminAuditLogsState {
  logs: AdminAuditLog[];
  filters: AdminAuditLogFilters;
  loading: boolean;
  error: string | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const defaultAdminAuditLogFilters: AdminAuditLogFilters = {
  query: null,
  actorUserId: null,
  actionType: null,
  entityType: null,
  entityId: null,
  status: null,
  fromDate: null,
  toDate: null,
  page: 0,
  size: 25
};

export const initialAdminAuditLogsState: AdminAuditLogsState = {
  logs: [],
  filters: defaultAdminAuditLogFilters,
  loading: false,
  error: null,
  page: 0,
  size: 25,
  totalElements: 0,
  totalPages: 0
};
