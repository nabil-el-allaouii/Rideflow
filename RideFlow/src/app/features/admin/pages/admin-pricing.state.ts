import { PricingConfig } from './admin-pricing.models';

export interface AdminPricingState {
  pricing: PricingConfig | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
  successMessage: string | null;
}

export const initialAdminPricingState: AdminPricingState = {
  pricing: null,
  loading: false,
  saving: false,
  error: null,
  successMessage: null
};
