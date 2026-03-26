import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AdminRentalFilters, AdminRentalPageResponse } from './admin-rentals.models';

@Injectable({ providedIn: 'root' })
export class AdminRentalsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: AdminRentalFilters): Observable<AdminRentalPageResponse> {
    const params = this.buildParams(filters);
    return this.http.get<AdminRentalPageResponse>(`${this.baseUrl}/rentals`, { params });
  }

  exportCsv(filters: AdminRentalFilters): Observable<Blob> {
    const params = this.buildParams(filters);
    return this.http.get(`${this.baseUrl}/rentals/export`, {
      params,
      responseType: 'blob'
    });
  }

  private buildParams(filters: AdminRentalFilters): HttpParams {
    let params = new HttpParams();

    if (filters.query) params = params.set('query', filters.query);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.userId !== null && filters.userId !== undefined) params = params.set('userId', filters.userId);
    if (filters.scooterId !== null && filters.scooterId !== undefined) params = params.set('scooterId', filters.scooterId);
    if (filters.page !== null && filters.page !== undefined) params = params.set('page', filters.page);
    if (filters.size !== null && filters.size !== undefined) params = params.set('size', filters.size);

    return params;
  }
}
