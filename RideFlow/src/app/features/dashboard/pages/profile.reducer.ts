import { createReducer, on } from '@ngrx/store';
import { profileActions } from './profile.actions';
import { initialProfileState, ProfileState } from './profile.state';

export const profileFeatureKey = 'profile';

export const profileReducer = createReducer<ProfileState>(
  initialProfileState,
  on(profileActions.loadRequested, (state) => ({
    ...state,
    loading: true,
    error: null,
    successMessage: null
  })),
  on(profileActions.loadSucceeded, (state, { profile }) => ({
    ...state,
    profile,
    loading: false,
    error: null
  })),
  on(profileActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(profileActions.updateRequested, (state) => ({
    ...state,
    saving: true,
    error: null,
    successMessage: null
  })),
  on(profileActions.updateSucceeded, (state, { profile }) => ({
    ...state,
    profile,
    saving: false,
    error: null,
    successMessage: 'Profile updated successfully.'
  })),
  on(profileActions.updateFailed, (state, { error }) => ({
    ...state,
    saving: false,
    error
  })),
  on(profileActions.clearFeedback, (state) => ({
    ...state,
    error: null,
    successMessage: null
  }))
);
