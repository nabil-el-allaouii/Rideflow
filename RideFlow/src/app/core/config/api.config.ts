import { InjectionToken } from '@angular/core';

function resolveApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return '/api';
  }

  const meta = document.querySelector('meta[name="rideflow-api-base-url"]');
  const configuredValue = meta?.getAttribute('content')?.trim();

  if (configuredValue && configuredValue.length > 0) {
    return configuredValue;
  }

  if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    return 'http://localhost:8080/api';
  }

  return '/api';
}

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => resolveApiBaseUrl()
});