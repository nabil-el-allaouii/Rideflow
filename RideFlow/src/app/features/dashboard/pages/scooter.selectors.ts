import { createFeatureSelector, createSelector } from '@ngrx/store';
import { scooterFeatureKey } from './scooter.reducer';
import { ScooterState } from './scooter.state';

export const selectScooterState = createFeatureSelector<ScooterState>(scooterFeatureKey);

export const selectAdminScooters = createSelector(selectScooterState, (state) => state.adminScooters);
export const selectAvailableScooters = createSelector(selectScooterState, (state) => state.availableScooters);
export const selectAdminLoading = createSelector(selectScooterState, (state) => state.adminLoading);
export const selectAvailableLoading = createSelector(selectScooterState, (state) => state.availableLoading);
export const selectAdminError = createSelector(selectScooterState, (state) => state.adminError);
export const selectAvailableError = createSelector(selectScooterState, (state) => state.availableError);
export const selectScooterMutationInProgress = createSelector(
  selectScooterState,
  (state) => state.mutationInProgress
);
export const selectScooterMutationError = createSelector(
  selectScooterState,
  (state) => state.mutationError
);
export const selectScooterMutationContext = createSelector(
  selectScooterState,
  (state) => state.mutationContext
);
export const selectLastScooterMutation = createSelector(
  selectScooterState,
  (state) => state.lastMutation
);
