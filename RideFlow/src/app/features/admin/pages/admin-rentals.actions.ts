import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AdminRentalFilters, AdminRentalPageResponse } from './admin-rentals.models';

export const adminRentalsActions = createActionGroup({
  source: 'Admin Rentals',
  events: {
    'Load Requested': props<{ filters: AdminRentalFilters }>(),
    'Load Succeeded': props<{ response: AdminRentalPageResponse; filters: AdminRentalFilters }>(),
    'Load Failed': props<{ error: string }>(),
    'Export Requested': props<{ filters: AdminRentalFilters }>(),
    'Export Succeeded': emptyProps(),
    'Export Failed': props<{ error: string }>()
  }
});
