import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { PricingConfig, PricingConfigRequest } from './admin-pricing.models';

@Injectable({ providedIn: 'root' })
export class AdminPricingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getCurrent(): Observable<PricingConfig> {
    return this.http.get<PricingConfig>(`${this.baseUrl}/admin/pricing/current`);
  }

  update(payload: PricingConfigRequest): Observable<PricingConfig> {
    return this.http.put<PricingConfig>(`${this.baseUrl}/admin/pricing`, payload);
  }
}
