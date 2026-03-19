import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import {
  FinalPaymentRequest,
  Payment,
  PaymentPageResponse,
  UnlockFeePaymentRequest
} from './payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  payUnlockFee(payload: UnlockFeePaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.baseUrl}/payments/unlock-fee`, payload);
  }

  payFinalPayment(payload: FinalPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.baseUrl}/payments/final-payment`, payload);
  }

  listMyPayments(page = 0, size = 50): Observable<PaymentPageResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PaymentPageResponse>(`${this.baseUrl}/payments/my-payments`, { params });
  }
}
