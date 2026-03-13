import { createActionGroup, emptyProps, props } from '@ngrx/store';
import {
  Scooter,
  ScooterFilters,
  ScooterFormPayload,
  ScooterPageResponse,
  ScooterStatus,
  ScooterUpdatePayload
} from './scooter.models';

export const scooterActions = createActionGroup({
  source: 'Scooter',
  events: {
    'Load Admin Requested': props<{ filters: ScooterFilters }>(),
    'Load Admin Succeeded': props<{ response: ScooterPageResponse; filters: ScooterFilters }>(),
    'Load Admin Failed': props<{ error: string }>(),
    'Load Available Requested': props<{ filters: ScooterFilters }>(),
    'Load Available Succeeded': props<{ response: ScooterPageResponse; filters: ScooterFilters }>(),
    'Load Available Failed': props<{ error: string }>(),
    'Create Requested': props<{ payload: ScooterFormPayload }>(),
    'Create Succeeded': props<{ scooter: Scooter }>(),
    'Create Failed': props<{ error: string }>(),
    'Update Requested': props<{ scooterId: number; payload: ScooterUpdatePayload }>(),
    'Update Succeeded': props<{ scooter: Scooter }>(),
    'Update Failed': props<{ error: string }>(),
    'Update Status Requested': props<{ scooterId: number; status: ScooterStatus }>(),
    'Update Status Succeeded': props<{ scooter: Scooter }>(),
    'Update Status Failed': props<{ error: string }>(),
    'Delete Requested': props<{ scooterId: number }>(),
    'Delete Succeeded': props<{ scooterId: number }>(),
    'Delete Failed': props<{ error: string }>(),
    'Clear Mutation Error': emptyProps()
  }
});
