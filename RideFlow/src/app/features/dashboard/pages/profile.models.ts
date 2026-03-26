export type ProfilePaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'WALLET';

export interface ProfileResponse {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  paymentMethod?: ProfilePaymentMethod | null;
  role: 'CUSTOMER' | 'ADMIN';
  status: 'ACTIVE' | 'SUSPENDED' | 'DISABLED';
  createdAt: string;
  lastLoginAt?: string | null;
  updatedAt: string;
}

export interface ProfileUpdateRequest {
  fullName: string;
  email: string;
  phoneNumber?: string | null;
  paymentMethod?: ProfilePaymentMethod | null;
}
