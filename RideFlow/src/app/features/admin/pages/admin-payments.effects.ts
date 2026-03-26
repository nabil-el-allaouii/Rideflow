import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, map, of } from 'rxjs';
import { adminPaymentsActions } from './admin-payments.actions';
import { AdminPaymentsApiService } from './admin-payments-api.service';

@Injectable()
export class AdminPaymentsEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(AdminPaymentsApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminPaymentsActions.loadRequested),
      exhaustMap(({ filters }) =>
        this.api.list(filters).pipe(
          map((response) => adminPaymentsActions.loadSucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(
              adminPaymentsActions.loadFailed({
                error: this.parseApiError(error, 'Unable to load payments.')
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
