import { AuthUser } from './auth.models';

export interface AuthState {
  isLoading: boolean;
  isAuthenticated: boolean;
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  error: string | null;
}

export const initialAuthState: AuthState = {
  isLoading: false,
  isAuthenticated: false,
  user: null,
  accessToken: null,
  refreshToken: null,
  error: null
};
