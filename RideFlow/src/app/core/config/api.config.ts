import { InjectionToken } from '@angular/core';

function resolveApiBaseUrl(): string {
  const meta = document.querySelector('meta[name="rideflow-api-base-url"]');
  const configuredValue = meta?.getAttribute('content')?.trim();

  return configuredValue && configuredValue.length > 0 ? configuredValue : 'http://localhost:8080/api';
}

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => resolveApiBaseUrl()
});
