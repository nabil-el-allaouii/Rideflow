import { createFeatureSelector, createSelector } from '@ngrx/store';
import { authFeatureKey } from './auth.reducer';
import { AuthState } from './auth.state';

export const selectAuthState = createFeatureSelector<AuthState>(authFeatureKey);

export const selectAuthLoading = createSelector(selectAuthState, (state) => state.isLoading);
export const selectAuthError = createSelector(selectAuthState, (state) => state.error);
export const selectIsAuthenticated = createSelector(selectAuthState, (state) => state.isAuthenticated);
export const selectCurrentUser = createSelector(selectAuthState, (state) => state.user);
export const selectAccessToken = createSelector(selectAuthState, (state) => state.accessToken);
export const selectRefreshToken = createSelector(selectAuthState, (state) => state.refreshToken);
