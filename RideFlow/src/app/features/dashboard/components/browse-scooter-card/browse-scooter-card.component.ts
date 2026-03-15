import { Component, EventEmitter, Input, Output } from '@angular/core';

export type BrowseScooterStatus = 'available' | 'low-battery' | 'in-use' | 'maintenance';

export interface BrowseScooterItem {
  id: number;
  name: string;
  code: string;
  speed: number;
  battery: number;
  distanceLabel: string;
  locationLabel: string;
  pricePerMinute: string;
  status: BrowseScooterStatus;
  unlockLabel: string;
  unlockDisabled: boolean;
}

@Component({
  selector: 'app-browse-scooter-card',
  imports: [],
  templateUrl: './browse-scooter-card.component.html',
  styleUrl: './browse-scooter-card.component.css'
})
export class BrowseScooterCardComponent {
  @Input({ required: true }) scooter!: BrowseScooterItem;
  @Output() unlock = new EventEmitter<number>();

  requestUnlock(): void {
    if (this.scooter.unlockDisabled) {
      return;
    }

    this.unlock.emit(this.scooter.id);
  }
}
