import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AdminAuditLogFilters, AdminAuditLogsPageResponse } from './admin-audit-logs.models';

@Injectable({ providedIn: 'root' })
export class AdminAuditLogsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: AdminAuditLogFilters): Observable<AdminAuditLogsPageResponse> {
    return this.http.get<AdminAuditLogsPageResponse>(`${this.baseUrl}/admin/audit-logs`, {
      params: this.buildParams(filters)
    });
  }

  private buildParams(filters: AdminAuditLogFilters): HttpParams {
    let params = new HttpParams();

    if (filters.query) params = params.set('query', filters.query);
    if (filters.actorUserId !== null && filters.actorUserId !== undefined) params = params.set('actorUserId', filters.actorUserId);
    if (filters.actionType) params = params.set('actionType', filters.actionType);
    if (filters.entityType) params = params.set('entityType', filters.entityType);
    if (filters.entityId !== null && filters.entityId !== undefined) params = params.set('entityId', filters.entityId);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.page !== null && filters.page !== undefined) params = params.set('page', filters.page);
    if (filters.size !== null && filters.size !== undefined) params = params.set('size', filters.size);

    return params;
  }
}
