import { Payment } from '../../dashboard/pages/payment.models';
import { PaymentStatus, PaymentType } from '../../dashboard/pages/rental.models';

export interface AdminPaymentFilters {
  query?: string | null;
  userId?: number | null;
  rentalId?: number | null;
  type?: PaymentType | null;
  status?: PaymentStatus | null;
  fromDate?: string | null;
  toDate?: string | null;
  page?: number | null;
  size?: number | null;
}

export interface AdminPaymentPageResponse {
  content: Payment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
