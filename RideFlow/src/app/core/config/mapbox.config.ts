import { InjectionToken } from '@angular/core';

export const MAPBOX_ACCESS_TOKEN_VALUE = 'pk.eyJ1IjoiYmFkcmVkZGluZTAwIiwiYSI6ImNsdzJ0cDJ1bTBtMnQyaW11NjBxczE3Z2kifQ.ockRcbgDpqVyMLsAv_tMgw';
export const MAPBOX_STYLE_URL_VALUE = 'mapbox://styles/mapbox/streets-v12';

export const MAPBOX_ACCESS_TOKEN = new InjectionToken<string>('MAPBOX_ACCESS_TOKEN', {
  providedIn: 'root',
  factory: () => MAPBOX_ACCESS_TOKEN_VALUE
});

export const MAPBOX_STYLE_URL = new InjectionToken<string>('MAPBOX_STYLE_URL', {
  providedIn: 'root',
  factory: () => MAPBOX_STYLE_URL_VALUE
});
