import { createFeatureSelector, createSelector } from '@ngrx/store';
import { paymentFeatureKey } from './payment.reducer';
import { PaymentState } from './payment.state';

export const selectPaymentState = createFeatureSelector<PaymentState>(paymentFeatureKey);

export const selectMyPayments = createSelector(selectPaymentState, (state) => state.payments);
export const selectPaymentLoading = createSelector(selectPaymentState, (state) => state.loading);
export const selectPaymentProcessing = createSelector(selectPaymentState, (state) => state.processing);
export const selectPaymentError = createSelector(selectPaymentState, (state) => state.error);
export const selectPaymentMutationContext = createSelector(
  selectPaymentState,
  (state) => state.mutationContext
);
export const selectLastProcessedPayment = createSelector(
  selectPaymentState,
  (state) => state.lastProcessedPayment
);
export const selectLastFailedRentalId = createSelector(
  selectPaymentState,
  (state) => state.lastFailedRentalId
);
export const selectLastPaymentMutation = createSelector(
  selectPaymentState,
  (state) => state.lastMutation
);
