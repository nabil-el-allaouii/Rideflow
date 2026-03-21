import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, concatMap, of } from 'rxjs';
import { authActions } from '../../auth/store/auth.actions';
import { AuthUser } from '../../auth/store/auth.models';
import { ProfileApiService } from './profile-api.service';
import { profileActions } from './profile.actions';
import { ProfileResponse } from './profile.models';

@Injectable()
export class ProfileEffects {
  private readonly actions$ = inject(Actions);
  private readonly profileApi = inject(ProfileApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(profileActions.loadRequested),
      concatMap(() =>
        this.profileApi.getCurrentProfile().pipe(
          concatMap((profile) => [
            profileActions.loadSucceeded({ profile }),
            authActions.userSynced({ user: this.toAuthUser(profile) })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(profileActions.loadFailed({ error: this.parseApiError(error, 'Unable to load profile.') }))
          )
        )
      )
    )
  );

  readonly update$ = createEffect(() =>
    this.actions$.pipe(
      ofType(profileActions.updateRequested),
      concatMap(({ payload }) =>
        this.profileApi.updateCurrentProfile(payload).pipe(
          concatMap((profile) => [
            profileActions.updateSucceeded({ profile }),
            authActions.userSynced({ user: this.toAuthUser(profile) })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(profileActions.updateFailed({ error: this.parseApiError(error, 'Unable to update profile.') }))
          )
        )
      )
    )
  );

  private toAuthUser(profile: ProfileResponse): AuthUser {
    return {
      id: profile.id,
      email: profile.email,
      fullName: profile.fullName,
      phoneNumber: profile.phoneNumber,
      paymentMethod: profile.paymentMethod,
      role: profile.role,
      status: profile.status
    };
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
