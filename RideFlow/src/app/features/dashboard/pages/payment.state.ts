import { Payment } from './payment.models';

export type PaymentMutationContext = 'unlock' | 'final' | null;

export interface PaymentMutationEvent {
  type: Exclude<PaymentMutationContext, null>;
  paymentId: number;
  rentalId: number;
  occurredAt: number;
}

export interface PaymentState {
  payments: Payment[];
  loading: boolean;
  processing: boolean;
  error: string | null;
  mutationContext: PaymentMutationContext;
  lastProcessedPayment: Payment | null;
  lastFailedRentalId: number | null;
  lastMutation: PaymentMutationEvent | null;
}

export const initialPaymentState: PaymentState = {
  payments: [],
  loading: false,
  processing: false,
  error: null,
  mutationContext: null,
  lastProcessedPayment: null,
  lastFailedRentalId: null,
  lastMutation: null
};
