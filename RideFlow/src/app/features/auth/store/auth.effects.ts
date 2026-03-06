import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { map, catchError, exhaustMap, of, tap } from 'rxjs';
import { AuthApiService } from '../data-access/auth-api.service';
import { AuthStorageService } from '../data-access/auth-storage.service';
import { authActions } from './auth.actions';
import { UserRole } from './auth.models';

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly authApi = inject(AuthApiService);
  private readonly storage = inject(AuthStorageService);
  private readonly router = inject(Router);

  readonly login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.loginRequested),
      exhaustMap(({ credentials }) =>
        this.authApi.login(credentials).pipe(
          map((response) => authActions.loginSucceeded({ response })),
          catchError((error: HttpErrorResponse) =>
            of(authActions.loginFailed({ error: this.parseApiError(error, 'Login failed.') }))
          )
        )
      )
    )
  );

  readonly register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.registerRequested),
      exhaustMap(({ payload }) =>
        this.authApi.register(payload).pipe(
          map((response) => authActions.registerSucceeded({ response })),
          catchError((error: HttpErrorResponse) =>
            of(
              authActions.registerFailed({
                error: this.parseApiError(error, 'Registration failed. Please try again.')
              })
            )
          )
        )
      )
    )
  );

  readonly persistSession$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(authActions.loginSucceeded, authActions.registerSucceeded),
        tap(({ response }) => {
          this.storage.saveSession(response);
          void this.router.navigateByUrl(this.resolvePostLoginRoute(response.user.role));
        })
      ),
    { dispatch: false }
  );

  readonly syncStoredUser$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(authActions.userSynced),
        tap(({ user }) => {
          this.storage.updateUser(user);
        })
      ),
    { dispatch: false }
  );

  readonly logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.logoutRequested),
      exhaustMap(({ refreshToken }) => {
        const request$ = refreshToken ? this.authApi.logout(refreshToken) : of(void 0);

        return request$.pipe(
          map(() => authActions.logoutCompleted()),
          catchError(() => of(authActions.logoutCompleted()))
        );
      })
    )
  );

  readonly clearSession$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(authActions.logoutCompleted),
        tap(() => {
          this.storage.clearSession();
          void this.router.navigate(['/auth/login']);
        })
      ),
    { dispatch: false }
  );

  private resolvePostLoginRoute(role: UserRole): string {
    const returnUrl = this.router.parseUrl(this.router.url).queryParams['returnUrl'];
    if (typeof returnUrl === 'string' && returnUrl.startsWith('/')) {
      return returnUrl;
    }

    return role === 'ADMIN' ? '/admin/dashboard' : '/dashboard';
  }

  private parseApiError(error: HttpErrorResponse, fallbackMessage: string): string {
    if (error.status === 0) {
      return 'Unable to reach the server. Check your Spring Boot API and try again.';
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }

    return fallbackMessage;
  }
}
