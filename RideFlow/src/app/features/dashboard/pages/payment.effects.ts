import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, concatMap, exhaustMap, map, of } from 'rxjs';
import { paymentActions } from './payment.actions';
import { PaymentApiService } from './payment-api.service';
import { rentalActions } from './rental.actions';
import { defaultRentalHistoryFilters } from './rental.state';

@Injectable()
export class PaymentEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(PaymentApiService);

  readonly loadMyPayments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(paymentActions.loadMyPaymentsRequested),
      exhaustMap(({ page = 0, size = 50 }) =>
        this.api.listMyPayments(page, size).pipe(
          map((response) => paymentActions.loadMyPaymentsSucceeded({ response })),
          catchError((error: HttpErrorResponse) =>
            of(
              paymentActions.loadMyPaymentsFailed({
                error: this.parseApiError(error, 'Unable to load payments.')
              })
            )
          )
        )
      )
    )
  );

  readonly processUnlock$ = createEffect(() =>
    this.actions$.pipe(
      ofType(paymentActions.processUnlockRequested),
      concatMap(({ payload }) =>
        this.api.payUnlockFee(payload).pipe(
          concatMap((payment) => [
            paymentActions.processUnlockSucceeded({ payment }),
            paymentActions.loadMyPaymentsRequested({ page: 0, size: 50 }),
            rentalActions.loadActiveRequested(),
            rentalActions.loadHistoryRequested({ filters: defaultRentalHistoryFilters })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(
              paymentActions.processUnlockFailed({
                error: this.parseApiError(error, 'Unable to process unlock payment.')
              })
            )
          )
        )
      )
    )
  );

  readonly processFinal$ = createEffect(() =>
    this.actions$.pipe(
      ofType(paymentActions.processFinalRequested),
      concatMap(({ payload }) =>
        this.api.payFinalPayment(payload).pipe(
          concatMap((payment) => [
            paymentActions.processFinalSucceeded({ payment }),
            paymentActions.loadMyPaymentsRequested({ page: 0, size: 50 }),
            rentalActions.loadActiveRequested(),
            rentalActions.loadHistoryRequested({ filters: defaultRentalHistoryFilters })
          ]),
          catchError((error: HttpErrorResponse) =>
            of(
              paymentActions.processFinalFailed({
                error: this.parseApiError(error, 'Unable to process final payment.'),
                rentalId: payload.rentalId
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
