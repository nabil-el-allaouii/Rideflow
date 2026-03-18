import { Component, EventEmitter, Input, Output } from '@angular/core';

export type RentalRecordStatus = 'completed' | 'cancelled' | 'force-ended';

export interface RentalHistoryRecord {
  rentalId: number;
  scooterName: string;
  rentalCode: string;
  dateLabel: string;
  durationLabel: string;
  distanceLabel: string;
  amountLabel: string;
  receiptAvailable: boolean;
  status: RentalRecordStatus;
}

@Component({
  selector: 'app-history-item',
  imports: [],
  templateUrl: './history-item.component.html',
  styleUrl: './history-item.component.css'
})
export class HistoryItemComponent {
  @Input({ required: true }) record!: RentalHistoryRecord;
  @Output() viewReceipt = new EventEmitter<number>();

  openReceipt(): void {
    this.viewReceipt.emit(this.record.rentalId);
  }
}
