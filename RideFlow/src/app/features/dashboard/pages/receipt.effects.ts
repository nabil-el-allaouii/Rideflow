import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, map, of } from 'rxjs';
import { ReceiptApiService } from './receipt-api.service';
import { receiptActions } from './receipt.actions';

@Injectable()
export class ReceiptEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(ReceiptApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(receiptActions.loadRequested),
      exhaustMap(({ rentalId }) =>
        this.api.getReceipt(rentalId).pipe(
          map((receipt) => receiptActions.loadSucceeded({ receipt })),
          catchError((error: HttpErrorResponse) =>
            of(
              receiptActions.loadFailed({
                error: this.parseApiError(error, 'Unable to load receipt.')
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
