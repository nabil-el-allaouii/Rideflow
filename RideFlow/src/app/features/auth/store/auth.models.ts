export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phoneNumber?: string | null;
}

export type UserRole = 'CUSTOMER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DISABLED';
export type PreferredPaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'WALLET';

export interface AuthUser {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  paymentMethod?: PreferredPaymentMethod | null;
  role: UserRole;
  status: UserStatus;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface StoredAuthSession extends AuthResponse {
  expiresAt: number;
}
