import { createFeatureSelector, createSelector } from '@ngrx/store';
import { profileFeatureKey } from './profile.reducer';
import { ProfileState } from './profile.state';

export const selectProfileState = createFeatureSelector<ProfileState>(profileFeatureKey);

export const selectCurrentProfile = createSelector(selectProfileState, (state) => state.profile);
export const selectProfileLoading = createSelector(selectProfileState, (state) => state.loading);
export const selectProfileSaving = createSelector(selectProfileState, (state) => state.saving);
export const selectProfileError = createSelector(selectProfileState, (state) => state.error);
export const selectProfileSuccessMessage = createSelector(
  selectProfileState,
  (state) => state.successMessage
);
