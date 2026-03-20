import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { Receipt } from './receipt.models';

@Injectable({ providedIn: 'root' })
export class ReceiptApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getReceipt(rentalId: number): Observable<Receipt> {
    return this.http.get<Receipt>(`${this.baseUrl}/receipts/${rentalId}`);
  }

  downloadPdf(rentalId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/receipts/${rentalId}/pdf`, {
      responseType: 'blob'
    });
  }
}
