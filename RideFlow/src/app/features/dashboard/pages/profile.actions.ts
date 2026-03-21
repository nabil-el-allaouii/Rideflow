import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { ProfileResponse, ProfileUpdateRequest } from './profile.models';

export const profileActions = createActionGroup({
  source: 'Profile',
  events: {
    'Load Requested': emptyProps(),
    'Load Succeeded': props<{ profile: ProfileResponse }>(),
    'Load Failed': props<{ error: string }>(),
    'Update Requested': props<{ payload: ProfileUpdateRequest }>(),
    'Update Succeeded': props<{ profile: ProfileResponse }>(),
    'Update Failed': props<{ error: string }>(),
    'Clear Feedback': emptyProps()
  }
});
