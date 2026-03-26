import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { adminAuditLogsActions } from './admin-audit-logs.actions';
import { AdminAuditLogFilters } from './admin-audit-logs.models';
import {
  selectAdminAuditLogs,
  selectAdminAuditLogsError,
  selectAdminAuditLogsFilters,
  selectAdminAuditLogsLoading,
  selectAdminAuditLogsPage,
  selectAdminAuditLogsSize,
  selectAdminAuditLogsTotalElements,
  selectAdminAuditLogsTotalPages
} from './admin-audit-logs.selectors';

@Injectable({ providedIn: 'root' })
export class AdminAuditLogsFacade {
  private readonly store = inject(Store);

  readonly logs$ = this.store.select(selectAdminAuditLogs);
  readonly filters$ = this.store.select(selectAdminAuditLogsFilters);
  readonly loading$ = this.store.select(selectAdminAuditLogsLoading);
  readonly error$ = this.store.select(selectAdminAuditLogsError);
  readonly page$ = this.store.select(selectAdminAuditLogsPage);
  readonly size$ = this.store.select(selectAdminAuditLogsSize);
  readonly totalElements$ = this.store.select(selectAdminAuditLogsTotalElements);
  readonly totalPages$ = this.store.select(selectAdminAuditLogsTotalPages);

  load(filters: AdminAuditLogFilters): void {
    this.store.dispatch(adminAuditLogsActions.loadRequested({ filters }));
  }
}
