import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, concatMap, exhaustMap, map, of } from 'rxjs';
import { rentalActions } from './rental.actions';
import { RentalApiService } from './rental-api.service';
import { defaultRentalHistoryFilters } from './rental.state';

@Injectable()
export class RentalEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(RentalApiService);

  readonly loadActive$ = createEffect(() =>
    this.actions$.pipe(
      ofType(rentalActions.loadActiveRequested),
      exhaustMap(() =>
        this.api.getActiveRental().pipe(
          map((activeRental) => rentalActions.loadActiveSucceeded({ activeRental })),
          catchError((error: HttpErrorResponse) =>
            of(rentalActions.loadActiveFailed({ error: this.parseApiError(error, 'Unable to load active rental.') }))
          )
        )
      )
    )
  );

  readonly loadHistory$ = createEffect(() =>
    this.actions$.pipe(
      ofType(rentalActions.loadHistoryRequested),
      exhaustMap(({ filters }) =>
        this.api.listMyRentals(filters).pipe(
          map((response) => rentalActions.loadHistorySucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(rentalActions.loadHistoryFailed({ error: this.parseApiError(error, 'Unable to load rental history.') }))
          )
        )
      )
    )
  );

  readonly unlock$ = createEffect(() =>
    this.actions$.pipe(
      ofType(rentalActions.unlockRequested),
      concatMap(({ payload }) =>
        this.api.unlock(payload).pipe(
          concatMap((rental) => [
            rentalActions.unlockSucceeded({ rental }),
            rentalActions.loadActiveRequested(),
            rentalActions.loadHistoryRequested({ filters: defaultRentalHistoryFilters })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(rentalActions.unlockFailed({ error: this.parseApiError(error, 'Unable to unlock scooter.') }))
          )
        )
      )
    )
  );

  readonly startRide$ = createEffect(() =>
    this.actions$.pipe(
      ofType(rentalActions.startRideRequested),
      concatMap(({ rentalId }) =>
        this.api.startRide(rentalId).pipe(
          concatMap((rental) => [
            rentalActions.startRideSucceeded({ rental }),
            rentalActions.loadActiveRequested()
          ]),
          catchError((error: HttpErrorResponse) =>
            of(rentalActions.startRideFailed({ error: this.parseApiError(error, 'Unable to start ride.') }))
          )
        )
      )
    )
  );

  readonly cancelRide$ = createEffect(() =>
    this.actions$.pipe(
      ofType(rentalActions.cancelRideRequested),
      concatMap(({ rentalId }) =>
        this.api.cancelRide(rentalId).pipe(
          concatMap((rental) => [
            rentalActions.cancelRideSucceeded({ rental }),
            rentalActions.loadActiveRequested(),
            rentalActions.loadHistoryRequested({ filters: defaultRentalHistoryFilters })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(rentalActions.cancelRideFailed({ error: this.parseApiError(error, 'Unable to cancel ride.') }))
          )
        )
      )
    )
  );

  readonly endRide$ = createEffect(() =>
    this.actions$.pipe(
      ofType(rentalActions.endRideRequested),
      concatMap(({ rentalId }) =>
        this.api.endRide(rentalId).pipe(
          concatMap((rental) => [
            rentalActions.endRideSucceeded({ rental }),
            rentalActions.loadActiveRequested(),
            rentalActions.loadHistoryRequested({ filters: defaultRentalHistoryFilters })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(rentalActions.endRideFailed({ error: this.parseApiError(error, 'Unable to end ride.') }))
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
