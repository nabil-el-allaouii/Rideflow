import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { FinalPaymentRequest, Payment, PaymentPageResponse, UnlockFeePaymentRequest } from './payment.models';

export const paymentActions = createActionGroup({
  source: 'Payment',
  events: {
    'Load My Payments Requested': props<{ page?: number; size?: number }>(),
    'Load My Payments Succeeded': props<{ response: PaymentPageResponse }>(),
    'Load My Payments Failed': props<{ error: string }>(),
    'Process Unlock Requested': props<{ payload: UnlockFeePaymentRequest }>(),
    'Process Unlock Succeeded': props<{ payment: Payment }>(),
    'Process Unlock Failed': props<{ error: string }>(),
    'Process Final Requested': props<{ payload: FinalPaymentRequest }>(),
    'Process Final Succeeded': props<{ payment: Payment }>(),
    'Process Final Failed': props<{ error: string; rentalId: number }>(),
    'Clear Status': emptyProps()
  }
});
