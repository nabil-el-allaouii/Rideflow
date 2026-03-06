import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { AuthStorageService } from '../data-access/auth-storage.service';
import { authActions } from './auth.actions';
import { LoginCredentials, RegisterRequest } from './auth.models';
import {
  selectAuthError,
  selectAuthLoading,
  selectCurrentUser,
  selectIsAuthenticated
} from './auth.selectors';

@Injectable({ providedIn: 'root' })
export class AuthFacade {
  private readonly store = inject(Store);
  private readonly storage = inject(AuthStorageService);

  readonly isLoading$ = this.store.select(selectAuthLoading);
  readonly error$ = this.store.select(selectAuthError);
  readonly user$ = this.store.select(selectCurrentUser);
  readonly isAuthenticated$ = this.store.select(selectIsAuthenticated);

  constructor() {
    const session = this.storage.getSession();

    if (session) {
      this.store.dispatch(authActions.hydrateSession({ session }));
    }
  }

  login(credentials: LoginCredentials): void {
    this.store.dispatch(authActions.loginRequested({ credentials }));
  }

  register(payload: RegisterRequest): void {
    this.store.dispatch(authActions.registerRequested({ payload }));
  }

  clearError(): void {
    this.store.dispatch(authActions.clearError());
  }

  logout(): void {
    this.store.dispatch(
      authActions.logoutRequested({ refreshToken: this.storage.getRefreshToken() })
    );
  }
}
