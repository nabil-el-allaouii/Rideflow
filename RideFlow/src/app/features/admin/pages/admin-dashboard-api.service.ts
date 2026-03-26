import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import {
  AdminDashboardStatistics,
  RentalReportPoint,
  RevenueReportPoint
} from './admin-dashboard.models';

@Injectable({ providedIn: 'root' })
export class AdminDashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getStatistics(): Observable<AdminDashboardStatistics> {
    return this.http.get<AdminDashboardStatistics>(`${this.baseUrl}/admin/statistics`);
  }

  getRentalsReport(fromDate: string, toDate: string): Observable<RentalReportPoint[]> {
    const params = new HttpParams().set('from', fromDate).set('to', toDate);
    return this.http.get<RentalReportPoint[]>(`${this.baseUrl}/admin/reports/rentals`, { params });
  }

  getRevenueReport(fromDate: string, toDate: string): Observable<RevenueReportPoint[]> {
    const params = new HttpParams().set('from', fromDate).set('to', toDate);
    return this.http.get<RevenueReportPoint[]>(`${this.baseUrl}/admin/reports/revenue`, { params });
  }
}
