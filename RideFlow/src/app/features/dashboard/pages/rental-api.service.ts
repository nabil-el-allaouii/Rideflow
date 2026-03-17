import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import {
  ActiveRental,
  Rental,
  RentalFilters,
  RentalPageResponse,
  UnlockScooterPayload
} from './rental.models';

@Injectable({ providedIn: 'root' })
export class RentalApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  unlock(payload: UnlockScooterPayload): Observable<Rental> {
    return this.http.post<Rental>(`${this.baseUrl}/rentals/unlock`, payload);
  }

  startRide(rentalId: number): Observable<Rental> {
    return this.http.post<Rental>(`${this.baseUrl}/rentals/${rentalId}/start`, {});
  }

  cancelRide(rentalId: number): Observable<Rental> {
    return this.http.post<Rental>(`${this.baseUrl}/rentals/${rentalId}/cancel`, {});
  }

  endRide(rentalId: number): Observable<Rental> {
    return this.http.post<Rental>(`${this.baseUrl}/rentals/${rentalId}/end`, {});
  }

  getActiveRental(): Observable<ActiveRental | null> {
    return this.http
      .get<ActiveRental | null>(`${this.baseUrl}/rentals/active`, { observe: 'response' })
      .pipe(map((response: HttpResponse<ActiveRental | null>) => response.body ?? null));
  }

  listMyRentals(filters: RentalFilters): Observable<RentalPageResponse> {
    let params = new HttpParams();

    if (filters.query) {
      params = params.set('query', filters.query);
    }

    if (filters.status) {
      params = params.set('status', filters.status);
    }

    if (filters.fromDate) {
      params = params.set('fromDate', filters.fromDate);
    }

    if (filters.toDate) {
      params = params.set('toDate', filters.toDate);
    }

    if (filters.page !== null && filters.page !== undefined) {
      params = params.set('page', filters.page);
    }

    if (filters.size !== null && filters.size !== undefined) {
      params = params.set('size', filters.size);
    }

    return this.http.get<RentalPageResponse>(`${this.baseUrl}/rentals/my-rentals`, { params });
  }
}
