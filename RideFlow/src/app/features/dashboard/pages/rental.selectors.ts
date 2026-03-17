import { createFeatureSelector, createSelector } from '@ngrx/store';
import { rentalFeatureKey } from './rental.reducer';
import { RentalState } from './rental.state';

export const selectRentalState = createFeatureSelector<RentalState>(rentalFeatureKey);

export const selectActiveRental = createSelector(selectRentalState, (state) => state.activeRental);
export const selectRentalHistory = createSelector(selectRentalState, (state) => state.history);
export const selectRentalActiveLoading = createSelector(
  selectRentalState,
  (state) => state.activeLoading
);
export const selectRentalHistoryLoading = createSelector(
  selectRentalState,
  (state) => state.historyLoading
);
export const selectRentalMutationInProgress = createSelector(
  selectRentalState,
  (state) => state.mutationInProgress
);
export const selectRentalError = createSelector(selectRentalState, (state) => state.error);
export const selectRentalMutationError = createSelector(
  selectRentalState,
  (state) => state.mutationError
);
export const selectLastRentalMutation = createSelector(
  selectRentalState,
  (state) => state.lastMutation
);
