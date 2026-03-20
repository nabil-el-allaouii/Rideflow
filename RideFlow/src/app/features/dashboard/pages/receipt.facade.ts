import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { receiptActions } from './receipt.actions';
import { selectCurrentReceipt, selectReceiptError, selectReceiptLoading } from './receipt.selectors';

@Injectable({ providedIn: 'root' })
export class ReceiptFacade {
  private readonly store = inject(Store);

  readonly currentReceipt$ = this.store.select(selectCurrentReceipt);
  readonly loading$ = this.store.select(selectReceiptLoading);
  readonly error$ = this.store.select(selectReceiptError);

  load(rentalId: number): void {
    this.store.dispatch(receiptActions.loadRequested({ rentalId }));
  }

  clear(): void {
    this.store.dispatch(receiptActions.clear());
  }
}
