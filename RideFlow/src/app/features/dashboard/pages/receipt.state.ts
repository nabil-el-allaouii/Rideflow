import { Receipt } from './receipt.models';

export interface ReceiptState {
  current: Receipt | null;
  loading: boolean;
  error: string | null;
}

export const initialReceiptState: ReceiptState = {
  current: null,
  loading: false,
  error: null
};
