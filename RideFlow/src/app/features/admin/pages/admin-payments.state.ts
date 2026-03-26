import { Payment } from '../../dashboard/pages/payment.models';
import { AdminPaymentFilters } from './admin-payments.models';

export interface AdminPaymentsState {
  payments: Payment[];
  filters: AdminPaymentFilters;
  loading: boolean;
  error: string | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const defaultAdminPaymentFilters: AdminPaymentFilters = {
  query: null,
  userId: null,
  rentalId: null,
  type: null,
  status: null,
  fromDate: null,
  toDate: null,
  page: 0,
  size: 25
};

export const initialAdminPaymentsState: AdminPaymentsState = {
  payments: [],
  filters: defaultAdminPaymentFilters,
  loading: false,
  error: null,
  page: 0,
  size: 25,
  totalElements: 0,
  totalPages: 0
};
