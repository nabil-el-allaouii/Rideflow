import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AdminPaymentFilters, AdminPaymentPageResponse } from './admin-payments.models';

@Injectable({ providedIn: 'root' })
export class AdminPaymentsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: AdminPaymentFilters): Observable<AdminPaymentPageResponse> {
    return this.http.get<AdminPaymentPageResponse>(`${this.baseUrl}/payments`, {
      params: this.buildParams(filters)
    });
  }

  private buildParams(filters: AdminPaymentFilters): HttpParams {
    let params = new HttpParams();

    if (filters.query) params = params.set('query', filters.query);
    if (filters.userId !== null && filters.userId !== undefined) params = params.set('userId', filters.userId);
    if (filters.rentalId !== null && filters.rentalId !== undefined) params = params.set('rentalId', filters.rentalId);
    if (filters.type) params = params.set('type', filters.type);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.page !== null && filters.page !== undefined) params = params.set('page', filters.page);
    if (filters.size !== null && filters.size !== undefined) params = params.set('size', filters.size);

    return params;
  }
}
