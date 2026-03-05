import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AuthResponse, LoginCredentials, RegisterRequest } from '../store/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  login(credentials: LoginCredentials): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, credentials);
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, payload);
  }

  refresh(refreshToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/refresh`, { refreshToken });
  }

  logout(refreshToken: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, { refreshToken });
  }
}
