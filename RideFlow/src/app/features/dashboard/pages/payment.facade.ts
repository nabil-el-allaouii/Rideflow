import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { paymentActions } from './payment.actions';
import { FinalPaymentRequest, UnlockFeePaymentRequest } from './payment.models';
import {
  selectLastFailedRentalId,
  selectLastPaymentMutation,
  selectLastProcessedPayment,
  selectMyPayments,
  selectPaymentError,
  selectPaymentLoading,
  selectPaymentMutationContext,
  selectPaymentProcessing
} from './payment.selectors';

@Injectable({ providedIn: 'root' })
export class PaymentFacade {
  private readonly store = inject(Store);

  readonly myPayments$ = this.store.select(selectMyPayments);
  readonly loading$ = this.store.select(selectPaymentLoading);
  readonly processing$ = this.store.select(selectPaymentProcessing);
  readonly error$ = this.store.select(selectPaymentError);
  readonly mutationContext$ = this.store.select(selectPaymentMutationContext);
  readonly lastProcessedPayment$ = this.store.select(selectLastProcessedPayment);
  readonly lastFailedRentalId$ = this.store.select(selectLastFailedRentalId);
  readonly lastMutation$ = this.store.select(selectLastPaymentMutation);

  loadMyPayments(page = 0, size = 50): void {
    this.store.dispatch(paymentActions.loadMyPaymentsRequested({ page, size }));
  }

  processUnlock(payload: UnlockFeePaymentRequest): void {
    this.store.dispatch(paymentActions.processUnlockRequested({ payload }));
  }

  processFinal(payload: FinalPaymentRequest): void {
    this.store.dispatch(paymentActions.processFinalRequested({ payload }));
  }

  clearStatus(): void {
    this.store.dispatch(paymentActions.clearStatus());
  }
}
