import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AdminUser, AdminUsersFilters, AdminUsersPageResponse } from './admin-users.models';
import { UserStatus } from '../../auth/store/auth.models';

export const adminUsersActions = createActionGroup({
  source: 'Admin Users',
  events: {
    'Load List Requested': props<{ filters: AdminUsersFilters }>(),
    'Load List Succeeded': props<{ response: AdminUsersPageResponse; filters: AdminUsersFilters }>(),
    'Load List Failed': props<{ error: string }>(),
    'Load Detail Requested': props<{ userId: number }>(),
    'Load Detail Succeeded': props<{ user: AdminUser }>(),
    'Load Detail Failed': props<{ error: string }>(),
    'Update Status Requested': props<{ userId: number; status: UserStatus }>(),
    'Update Status Succeeded': props<{ user: AdminUser }>(),
    'Update Status Failed': props<{ error: string }>(),
    'Clear Mutation Error': emptyProps()
  }
});
