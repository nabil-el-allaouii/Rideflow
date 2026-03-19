import { createReducer, on } from '@ngrx/store';
import { paymentActions } from './payment.actions';
import { initialPaymentState, PaymentMutationContext, PaymentState } from './payment.state';

export const paymentFeatureKey = 'payments';

export const paymentReducer = createReducer<PaymentState>(
  initialPaymentState,
  on(paymentActions.loadMyPaymentsRequested, (state) => ({
    ...state,
    loading: true,
    error: null
  })),
  on(paymentActions.loadMyPaymentsSucceeded, (state, { response }) => ({
    ...state,
    loading: false,
    payments: response.content
  })),
  on(paymentActions.loadMyPaymentsFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(paymentActions.processUnlockRequested, (state) => setProcessingState(state, 'unlock')),
  on(paymentActions.processFinalRequested, (state) => setProcessingState(state, 'final')),
  on(paymentActions.processUnlockSucceeded, (state, { payment }) => ({
    ...state,
    processing: false,
    mutationContext: null,
    error: null,
    lastFailedRentalId: null,
    lastProcessedPayment: payment,
    lastMutation: buildMutation('unlock', payment)
  })),
  on(paymentActions.processFinalSucceeded, (state, { payment }) => ({
    ...state,
    processing: false,
    mutationContext: null,
    error: null,
    lastFailedRentalId: null,
    lastProcessedPayment: payment,
    lastMutation: buildMutation('final', payment),
    payments: [payment, ...state.payments.filter((item) => item.id !== payment.id)]
  })),
  on(paymentActions.processUnlockFailed, (state, { error }) => ({
    ...state,
    processing: false,
    mutationContext: 'unlock',
    error
  })),
  on(paymentActions.processFinalFailed, (state, { error, rentalId }) => ({
    ...state,
    processing: false,
    mutationContext: 'final',
    error,
    lastFailedRentalId: rentalId
  })),
  on(paymentActions.clearStatus, (state) => ({
    ...state,
    error: null,
    mutationContext: null,
    lastProcessedPayment: null,
    lastFailedRentalId: null
  }))
);

function setProcessingState(state: PaymentState, context: PaymentMutationContext): PaymentState {
  return {
    ...state,
    processing: true,
    mutationContext: context,
    error: null,
    lastProcessedPayment: null,
    lastFailedRentalId: context === 'final' ? state.lastFailedRentalId : null
  };
}

function buildMutation(type: Exclude<PaymentMutationContext, null>, payment: { id: number; rentalId: number }) {
  return {
    type,
    paymentId: payment.id,
    rentalId: payment.rentalId,
    occurredAt: Date.now()
  };
}
