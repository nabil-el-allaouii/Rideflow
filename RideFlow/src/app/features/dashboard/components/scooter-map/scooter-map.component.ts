import { Component, Input, inject } from '@angular/core';
import {
  ControlComponent,
  MapComponent,
  MarkerComponent,
  NavigationControlDirective,
  PopupComponent
} from 'ngx-mapbox-gl';
import { LngLatBoundsLike } from 'mapbox-gl';
import { MAPBOX_ACCESS_TOKEN, MAPBOX_STYLE_URL } from '../../../../core/config/mapbox.config';

export interface ScooterMapMarker {
  code: string;
  name: string;
  distanceLabel: string;
  battery: number;
  longitude: number;
  latitude: number;
  area: string;
}

@Component({
  selector: 'app-scooter-map',
  imports: [MapComponent, MarkerComponent, PopupComponent, ControlComponent, NavigationControlDirective],
  templateUrl: './scooter-map.component.html',
  styleUrl: './scooter-map.component.css'
})
export class ScooterMapComponent {
  private readonly mapboxToken = inject(MAPBOX_ACCESS_TOKEN);
  readonly mapStyle = inject(MAPBOX_STYLE_URL);

  @Input({ required: true }) markers: ScooterMapMarker[] = [];

  readonly mapTokenMissing = !this.mapboxToken;
  readonly fitBoundsOptions = {
    padding: { top: 48, right: 48, bottom: 48, left: 48 },
    maxZoom: 14
  } as const;

  get center(): [number, number] {
    if (!this.markers.length) {
      return [-7.603869, 33.589886];
    }

    return [this.markers[0].longitude, this.markers[0].latitude];
  }

  get fitBounds(): LngLatBoundsLike | undefined {
    if (!this.markers.length) {
      return undefined;
    }

    let minLng = this.markers[0].longitude;
    let maxLng = this.markers[0].longitude;
    let minLat = this.markers[0].latitude;
    let maxLat = this.markers[0].latitude;

    this.markers.forEach((marker) => {
      minLng = Math.min(minLng, marker.longitude);
      maxLng = Math.max(maxLng, marker.longitude);
      minLat = Math.min(minLat, marker.latitude);
      maxLat = Math.max(maxLat, marker.latitude);
    });

    return [
      [minLng, minLat],
      [maxLng, maxLat]
    ];
  }
}

