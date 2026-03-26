import { PaymentMethod, PaymentStatus, PaymentType, RentalStatus } from './rental.models';

export interface Payment {
  id: number;
  rentalId: number;
  userId: number;
  userFullName?: string | null;
  userEmail?: string | null;
  scooterId?: number | null;
  scooterCode?: string | null;
  scooterModel?: string | null;
  rentalStatus: RentalStatus;
  type: PaymentType;
  amount: number;
  status: PaymentStatus;
  paymentMethod: PaymentMethod;
  transactionReference: string;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentPageResponse {
  content: Payment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UnlockFeePaymentRequest {
  scooterId: number;
}

export interface FinalPaymentRequest {
  rentalId: number;
}
