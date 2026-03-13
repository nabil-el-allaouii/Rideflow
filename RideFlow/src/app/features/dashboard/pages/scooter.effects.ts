import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, concatMap, exhaustMap, map, of } from 'rxjs';
import { ScooterApiService } from './scooter-api.service';
import { scooterActions } from './scooter.actions';
import { defaultAdminScooterFilters } from './scooter.state';

@Injectable()
export class ScooterEffects {
  private readonly actions$ = inject(Actions);
  private readonly scooterApi = inject(ScooterApiService);

  readonly loadAdmin$ = createEffect(() =>
    this.actions$.pipe(
      ofType(scooterActions.loadAdminRequested),
      exhaustMap(({ filters }) =>
        this.scooterApi.listAdmin(filters).pipe(
          map((response) => scooterActions.loadAdminSucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(
              scooterActions.loadAdminFailed({
                error: this.parseApiError(error, 'Unable to load scooters.')
              })
            )
          )
        )
      )
    )
  );

  readonly loadAvailable$ = createEffect(() =>
    this.actions$.pipe(
      ofType(scooterActions.loadAvailableRequested),
      exhaustMap(({ filters }) =>
        this.scooterApi.listAvailable(filters).pipe(
          map((response) => scooterActions.loadAvailableSucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(
              scooterActions.loadAvailableFailed({
                error: this.parseApiError(error, 'Unable to load scooters.')
              })
            )
          )
        )
      )
    )
  );

  readonly create$ = createEffect(() =>
    this.actions$.pipe(
      ofType(scooterActions.createRequested),
      concatMap(({ payload }) =>
        this.scooterApi.create(payload).pipe(
          concatMap((scooter) => [
            scooterActions.createSucceeded({ scooter }),
            scooterActions.loadAdminRequested({ filters: defaultAdminScooterFilters })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(
              scooterActions.createFailed({
                error: this.parseApiError(error, 'Unable to save scooter.')
              })
            )
          )
        )
      )
    )
  );

  readonly update$ = createEffect(() =>
    this.actions$.pipe(
      ofType(scooterActions.updateRequested),
      concatMap(({ scooterId, payload }) =>
        this.scooterApi.update(scooterId, payload).pipe(
          map((scooter) => scooterActions.updateSucceeded({ scooter })),
          catchError((error: HttpErrorResponse) =>
            of(
              scooterActions.updateFailed({
                error: this.parseApiError(error, 'Unable to save scooter.')
              })
            )
          )
        )
      )
    )
  );

  readonly updateStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(scooterActions.updateStatusRequested),
      concatMap(({ scooterId, status }) =>
        this.scooterApi.updateStatus(scooterId, status).pipe(
          map((scooter) => scooterActions.updateStatusSucceeded({ scooter })),
          catchError((error: HttpErrorResponse) =>
            of(
              scooterActions.updateStatusFailed({
                error: this.parseApiError(error, 'Unable to update scooter status.')
              })
            )
          )
        )
      )
    )
  );

  readonly delete$ = createEffect(() =>
    this.actions$.pipe(
      ofType(scooterActions.deleteRequested),
      concatMap(({ scooterId }) =>
        this.scooterApi.delete(scooterId).pipe(
          map(() => scooterActions.deleteSucceeded({ scooterId })),
          catchError((error: HttpErrorResponse) =>
            of(
              scooterActions.deleteFailed({
                error: this.parseApiError(error, 'Unable to delete scooter.')
              })
            )
          )
        )
      )
    )
  );

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
