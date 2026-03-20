import { createReducer, on } from '@ngrx/store';
import { receiptActions } from './receipt.actions';
import { initialReceiptState, ReceiptState } from './receipt.state';

export const receiptFeatureKey = 'receipt';

export const receiptReducer = createReducer<ReceiptState>(
  initialReceiptState,
  on(receiptActions.loadRequested, (state) => ({
    ...state,
    loading: true,
    error: null
  })),
  on(receiptActions.loadSucceeded, (state, { receipt }) => ({
    ...state,
    current: receipt,
    loading: false,
    error: null
  })),
  on(receiptActions.loadFailed, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(receiptActions.clear, () => ({
    ...initialReceiptState
  }))
);
