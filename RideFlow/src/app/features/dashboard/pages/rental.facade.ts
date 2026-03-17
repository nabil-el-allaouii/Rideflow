import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { rentalActions } from './rental.actions';
import { RentalFilters, UnlockScooterPayload } from './rental.models';
import {
  selectActiveRental,
  selectLastRentalMutation,
  selectRentalActiveLoading,
  selectRentalError,
  selectRentalHistory,
  selectRentalHistoryLoading,
  selectRentalMutationError,
  selectRentalMutationInProgress
} from './rental.selectors';

@Injectable({ providedIn: 'root' })
export class RentalFacade {
  private readonly store = inject(Store);

  readonly activeRental$ = this.store.select(selectActiveRental);
  readonly history$ = this.store.select(selectRentalHistory);
  readonly activeLoading$ = this.store.select(selectRentalActiveLoading);
  readonly historyLoading$ = this.store.select(selectRentalHistoryLoading);
  readonly mutationInProgress$ = this.store.select(selectRentalMutationInProgress);
  readonly error$ = this.store.select(selectRentalError);
  readonly mutationError$ = this.store.select(selectRentalMutationError);
  readonly lastMutation$ = this.store.select(selectLastRentalMutation);

  loadActive(): void {
    this.store.dispatch(rentalActions.loadActiveRequested());
  }

  loadHistory(filters: RentalFilters): void {
    this.store.dispatch(rentalActions.loadHistoryRequested({ filters }));
  }

  unlock(payload: UnlockScooterPayload): void {
    this.store.dispatch(rentalActions.unlockRequested({ payload }));
  }

  cancelRide(rentalId: number): void {
    this.store.dispatch(rentalActions.cancelRideRequested({ rentalId }));
  }

  startRide(rentalId: number): void {
    this.store.dispatch(rentalActions.startRideRequested({ rentalId }));
  }

  endRide(rentalId: number): void {
    this.store.dispatch(rentalActions.endRideRequested({ rentalId }));
  }

  clearMutationError(): void {
    this.store.dispatch(rentalActions.clearMutationError());
  }
}
