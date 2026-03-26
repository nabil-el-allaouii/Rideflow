export type RentalStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'FORCE_ENDED';
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'WALLET';
export type PaymentType = 'UNLOCK_FEE' | 'FINAL_PAYMENT';
export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED';

export interface Rental {
  id: number;
  userId: number;
  scooterId: number;
  scooterCode: string;
  scooterModel: string;
  startTime: string | null;
  endTime: string | null;
  status: RentalStatus;
  batteryAtStart: number | null;
  batteryAtEnd: number | null;
  distanceTraveled: number | null;
  durationMinutes: number | null;
  unlockFeeApplied: number;
  ratePerMinuteApplied: number;
  totalCost: number | null;
  receiptAvailable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ActiveRental {
  rentalId: number;
  scooterId: number;
  scooterCode: string;
  scooterModel: string;
  status: RentalStatus;
  createdAt: string;
  startTime: string | null;
}

export interface RentalFilters {
  query?: string | null;
  status?: RentalStatus | null;
  fromDate?: string | null;
  toDate?: string | null;
  page?: number | null;
  size?: number | null;
}

export interface RentalPageResponse {
  content: Rental[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UnlockScooterPayload {
  scooterId: number;
}
