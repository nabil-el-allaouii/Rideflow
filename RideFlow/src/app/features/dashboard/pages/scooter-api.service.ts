import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import {
  Scooter,
  ScooterFilters,
  ScooterFormPayload,
  ScooterPageResponse,
  ScooterStatus,
  ScooterUpdatePayload
} from './scooter.models';

@Injectable({ providedIn: 'root' })
export class ScooterApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listAdmin(filters: ScooterFilters = {}): Observable<ScooterPageResponse> {
    return this.http.get<ScooterPageResponse>(`${this.baseUrl}/scooters`, {
      params: this.buildParams(filters)
    });
  }

  listAvailable(filters: ScooterFilters = {}): Observable<ScooterPageResponse> {
    return this.http.get<ScooterPageResponse>(`${this.baseUrl}/scooters/available`, {
      params: this.buildParams(filters)
    });
  }

  getById(id: number): Observable<Scooter> {
    return this.http.get<Scooter>(`${this.baseUrl}/scooters/${id}`);
  }

  create(payload: ScooterFormPayload): Observable<Scooter> {
    return this.http.post<Scooter>(`${this.baseUrl}/scooters`, payload);
  }

  update(id: number, payload: ScooterUpdatePayload): Observable<Scooter> {
    return this.http.put<Scooter>(`${this.baseUrl}/scooters/${id}`, payload);
  }

  updateStatus(id: number, status: ScooterStatus): Observable<Scooter> {
    return this.http.put<Scooter>(`${this.baseUrl}/scooters/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/scooters/${id}`);
  }

  private buildParams(filters: ScooterFilters): HttpParams {
    let params = new HttpParams();

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== null && value !== undefined && `${value}`.trim() !== '') {
        params = params.set(key, `${value}`);
      }
    });

    return params;
  }
}
