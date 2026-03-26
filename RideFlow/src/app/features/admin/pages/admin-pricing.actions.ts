import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { PricingConfig, PricingConfigRequest } from './admin-pricing.models';

export const adminPricingActions = createActionGroup({
  source: 'Admin Pricing',
  events: {
    'Load Requested': emptyProps(),
    'Load Succeeded': props<{ pricing: PricingConfig }>(),
    'Load Failed': props<{ error: string }>(),
    'Update Requested': props<{ payload: PricingConfigRequest }>(),
    'Update Succeeded': props<{ pricing: PricingConfig }>(),
    'Update Failed': props<{ error: string }>(),
    'Clear Feedback': emptyProps()
  }
});
