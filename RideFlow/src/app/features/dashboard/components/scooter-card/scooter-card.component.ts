import { Component, Input } from '@angular/core';

export type ScooterStatus = 'available' | 'low-battery';

export interface ScooterItem {
  name: string;
  code: string;
  battery: number;
  distanceLabel: string;
  status: ScooterStatus;
}

@Component({
  selector: 'app-scooter-card',
  imports: [],
  templateUrl: './scooter-card.component.html',
  styleUrl: './scooter-card.component.css'
})
export class ScooterCardComponent {
  @Input({ required: true }) scooter!: ScooterItem;
}
