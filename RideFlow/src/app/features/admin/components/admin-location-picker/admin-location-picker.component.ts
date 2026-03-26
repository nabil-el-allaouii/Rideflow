import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import {
  ControlComponent,
  MapComponent,
  MarkerComponent,
  NavigationControlDirective,
  PopupComponent
} from 'ngx-mapbox-gl';
import { MapMouseEvent, Marker } from 'mapbox-gl';
import { MAPBOX_ACCESS_TOKEN, MAPBOX_STYLE_URL } from '../../../../core/config/mapbox.config';

export interface AdminLocationSelection {
  latitude: number;
  longitude: number;
}

@Component({
  selector: 'app-admin-location-picker',
  imports: [MapComponent, MarkerComponent, PopupComponent, ControlComponent, NavigationControlDirective],
  templateUrl: './admin-location-picker.component.html',
  styleUrl: './admin-location-picker.component.css'
})
export class AdminLocationPickerComponent {
  private readonly mapboxToken = inject(MAPBOX_ACCESS_TOKEN);
  readonly mapStyle = inject(MAPBOX_STYLE_URL);

  @Input() latitude: number | null = null;
  @Input() longitude: number | null = null;
  @Output() locationSelected = new EventEmitter<AdminLocationSelection>();

  readonly defaultCenter: [number, number] = [-7.603869, 33.589886];
  readonly mapTokenMissing = !this.mapboxToken;

  get center(): [number, number] {
    if (this.longitude === null || this.latitude === null) {
      return this.defaultCenter;
    }

    return [this.longitude, this.latitude];
  }

  get markerCoords(): [number, number] | null {
    if (this.longitude === null || this.latitude === null) {
      return null;
    }

    return [this.longitude, this.latitude];
  }

  onMapClick(event: MapMouseEvent): void {
    this.emitLocation(event.lngLat.lat, event.lngLat.lng);
  }

  onMarkerDragEnd(marker: Marker): void {
    const lngLat = marker.getLngLat();
    this.emitLocation(lngLat.lat, lngLat.lng);
  }

  private emitLocation(latitude: number, longitude: number): void {
    this.locationSelected.emit({
      latitude: Number(latitude.toFixed(6)),
      longitude: Number(longitude.toFixed(6))
    });
  }
}
