import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import {
  AdminUser,
  AdminUsersFilters,
  AdminUsersPageResponse,
  AdminUserStatusUpdateRequest
} from './admin-users.models';

@Injectable({ providedIn: 'root' })
export class AdminUsersApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: AdminUsersFilters): Observable<AdminUsersPageResponse> {
    let params = new HttpParams();

    if (filters.query) {
      params = params.set('query', filters.query);
    }

    if (filters.role) {
      params = params.set('role', filters.role);
    }

    if (filters.status) {
      params = params.set('status', filters.status);
    }

    if (filters.page !== null && filters.page !== undefined) {
      params = params.set('page', filters.page);
    }

    if (filters.size !== null && filters.size !== undefined) {
      params = params.set('size', filters.size);
    }

    return this.http.get<AdminUsersPageResponse>(`${this.baseUrl}/admin/users`, { params });
  }

  getById(userId: number): Observable<AdminUser> {
    return this.http.get<AdminUser>(`${this.baseUrl}/admin/users/${userId}`);
  }

  updateStatus(userId: number, payload: AdminUserStatusUpdateRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/admin/users/${userId}/status`, payload);
  }
}
