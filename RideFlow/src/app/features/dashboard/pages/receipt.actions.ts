import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Receipt } from './receipt.models';

export const receiptActions = createActionGroup({
  source: 'Receipt',
  events: {
    'Load Requested': props<{ rentalId: number }>(),
    'Load Succeeded': props<{ receipt: Receipt }>(),
    'Load Failed': props<{ error: string }>(),
    'Clear': emptyProps()
  }
});
