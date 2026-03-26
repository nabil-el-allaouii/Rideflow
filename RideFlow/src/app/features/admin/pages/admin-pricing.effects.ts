import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, map, of } from 'rxjs';
import { adminPricingActions } from './admin-pricing.actions';
import { AdminPricingApiService } from './admin-pricing-api.service';

@Injectable()
export class AdminPricingEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(AdminPricingApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminPricingActions.loadRequested),
      exhaustMap(() =>
        this.api.getCurrent().pipe(
          map((pricing) => adminPricingActions.loadSucceeded({ pricing })),
          catchError((error: HttpErrorResponse) =>
            of(adminPricingActions.loadFailed({ error: this.parseApiError(error, 'Unable to load pricing.') }))
          )
        )
      )
    )
  );

  readonly update$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminPricingActions.updateRequested),
      exhaustMap(({ payload }) =>
        this.api.update(payload).pipe(
          map((pricing) => adminPricingActions.updateSucceeded({ pricing })),
          catchError((error: HttpErrorResponse) =>
            of(adminPricingActions.updateFailed({ error: this.parseApiError(error, 'Unable to update pricing.') }))
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
