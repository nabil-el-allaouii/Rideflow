import { createActionGroup, emptyProps, props } from '@ngrx/store';
import {
  ActiveRental,
  Rental,
  RentalFilters,
  RentalPageResponse,
  UnlockScooterPayload
} from './rental.models';

export const rentalActions = createActionGroup({
  source: 'Rental',
  events: {
    'Load Active Requested': emptyProps(),
    'Load Active Succeeded': props<{ activeRental: ActiveRental | null }>(),
    'Load Active Failed': props<{ error: string }>(),
    'Load History Requested': props<{ filters: RentalFilters }>(),
    'Load History Succeeded': props<{ response: RentalPageResponse; filters: RentalFilters }>(),
    'Load History Failed': props<{ error: string }>(),
    'Unlock Requested': props<{ payload: UnlockScooterPayload }>(),
    'Unlock Succeeded': props<{ rental: Rental }>(),
    'Unlock Failed': props<{ error: string }>(),
    'Cancel Ride Requested': props<{ rentalId: number }>(),
    'Cancel Ride Succeeded': props<{ rental: Rental }>(),
    'Cancel Ride Failed': props<{ error: string }>(),
    'Start Ride Requested': props<{ rentalId: number }>(),
    'Start Ride Succeeded': props<{ rental: Rental }>(),
    'Start Ride Failed': props<{ error: string }>(),
    'End Ride Requested': props<{ rentalId: number }>(),
    'End Ride Succeeded': props<{ rental: Rental }>(),
    'End Ride Failed': props<{ error: string }>(),
    'Clear Mutation Error': emptyProps()
  }
});
