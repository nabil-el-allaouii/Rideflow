import { createActionGroup, props } from '@ngrx/store';
import { AdminPaymentFilters, AdminPaymentPageResponse } from './admin-payments.models';

export const adminPaymentsActions = createActionGroup({
  source: 'Admin Payments',
  events: {
    'Load Requested': props<{ filters: AdminPaymentFilters }>(),
    'Load Succeeded': props<{ response: AdminPaymentPageResponse; filters: AdminPaymentFilters }>(),
    'Load Failed': props<{ error: string }>()
  }
});
