import { createReducer, on } from '@ngrx/store';
import { authActions } from './auth.actions';
import { AuthState, initialAuthState } from './auth.state';

export const authFeatureKey = 'auth';

export const authReducer = createReducer<AuthState>(
  initialAuthState,
  on(authActions.hydrateSession, (state, { session }) => ({
    ...state,
    isAuthenticated: true,
    user: session.user,
    accessToken: session.accessToken,
    refreshToken: session.refreshToken,
    error: null
  })),
  on(authActions.loginRequested, authActions.registerRequested, (state) => ({
    ...state,
    isLoading: true,
    error: null
  })),
  on(authActions.loginSucceeded, authActions.registerSucceeded, (state, { response }) => ({
    ...state,
    isLoading: false,
    isAuthenticated: true,
    user: response.user,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    error: null
  })),
  on(authActions.loginFailed, authActions.registerFailed, (state, { error }) => ({
    ...state,
    isLoading: false,
    error
  })),
  on(authActions.userSynced, (state, { user }) => ({
    ...state,
    user
  })),
  on(authActions.clearError, (state) => ({
    ...state,
    error: null
  })),
  on(authActions.logoutCompleted, () => ({
    ...initialAuthState
  }))
);
