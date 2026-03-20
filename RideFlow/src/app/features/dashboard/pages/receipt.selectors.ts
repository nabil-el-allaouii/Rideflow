import { createFeatureSelector, createSelector } from '@ngrx/store';
import { receiptFeatureKey } from './receipt.reducer';
import { ReceiptState } from './receipt.state';

export const selectReceiptState = createFeatureSelector<ReceiptState>(receiptFeatureKey);

export const selectCurrentReceipt = createSelector(selectReceiptState, (state) => state.current);
export const selectReceiptLoading = createSelector(selectReceiptState, (state) => state.loading);
export const selectReceiptError = createSelector(selectReceiptState, (state) => state.error);
