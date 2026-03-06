import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AuthResponse, AuthUser, LoginCredentials, RegisterRequest, StoredAuthSession } from './auth.models';

export const authActions = createActionGroup({
  source: 'Auth',
  events: {
    'Hydrate Session': props<{ session: StoredAuthSession }>(),
    'Login Requested': props<{ credentials: LoginCredentials }>(),
    'Login Succeeded': props<{ response: AuthResponse }>(),
    'Login Failed': props<{ error: string }>(),
    'Register Requested': props<{ payload: RegisterRequest }>(),
    'Register Succeeded': props<{ response: AuthResponse }>(),
    'Register Failed': props<{ error: string }>(),
    'User Synced': props<{ user: AuthUser }>(),
    'Clear Error': emptyProps(),
    'Logout Requested': props<{ refreshToken: string | null }>(),
    'Logout Completed': emptyProps()
  }
});
