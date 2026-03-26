import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, map, of, tap } from 'rxjs';
import { adminRentalsActions } from './admin-rentals.actions';
import { AdminRentalsApiService } from './admin-rentals-api.service';

@Injectable()
export class AdminRentalsEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(AdminRentalsApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminRentalsActions.loadRequested),
      exhaustMap(({ filters }) =>
        this.api.list(filters).pipe(
          map((response) => adminRentalsActions.loadSucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(
              adminRentalsActions.loadFailed({
                error: this.parseApiError(error, 'Unable to load rentals.')
              })
            )
          )
        )
      )
    )
  );

  readonly export$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminRentalsActions.exportRequested),
      exhaustMap(({ filters }) =>
        this.api.exportCsv(filters).pipe(
          tap((blob) => {
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = 'rentals.csv';
            anchor.click();
            URL.revokeObjectURL(url);
          }),
          map(() => adminRentalsActions.exportSucceeded()),
          catchError((error: HttpErrorResponse) =>
            of(
              adminRentalsActions.exportFailed({
                error: this.parseApiError(error, 'Unable to export rentals.')
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
