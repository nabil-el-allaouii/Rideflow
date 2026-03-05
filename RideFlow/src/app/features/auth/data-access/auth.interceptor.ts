import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AuthStorageService } from './auth-storage.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const baseUrl = inject(API_BASE_URL);
  const storage = inject(AuthStorageService);
  const accessToken = storage.getAccessToken();

  if (!accessToken || !req.url.startsWith(baseUrl) || req.url.includes('/auth/refresh')) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    })
  );
};
