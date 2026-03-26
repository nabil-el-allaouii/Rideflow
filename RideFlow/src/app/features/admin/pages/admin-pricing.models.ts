export interface PricingConfig {
  id: number;
  unlockFee: number;
  ratePerMinute: number;
  batteryConsumptionRate: number;
  currency: string;
  effectiveFrom: string;
  active: boolean;
}

export interface PricingConfigRequest {
  unlockFee: number;
  ratePerMinute: number;
  batteryConsumptionRate: number;
  currency: string;
}
