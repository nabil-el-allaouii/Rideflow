import { PaymentStatus } from './rental.models';

export interface Receipt {
  id: number;
  receiptCode: string;
  rentalId: number;
  scooterCode: string;
  scooterModel: string;
  userFullName: string;
  userEmail: string;
  startTime: string | null;
  endTime: string | null;
  durationMinutes: number | null;
  durationLabel: string;
  unlockFee: number;
  ratePerMinute: number;
  usageCost: number;
  totalCost: number;
  batteryAtStart: number;
  batteryAtEnd: number;
  batteryConsumed: number;
  distanceTraveled: number;
  unlockPaymentStatus: PaymentStatus | null;
  unlockPaymentReference: string | null;
  finalPaymentStatus: PaymentStatus | null;
  finalPaymentReference: string | null;
  finalPaymentFailureReason: string | null;
  generatedAt: string;
}
