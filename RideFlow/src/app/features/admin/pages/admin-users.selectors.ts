import { createFeatureSelector, createSelector } from '@ngrx/store';
import { adminUsersFeatureKey } from './admin-users.reducer';
import { AdminUsersState } from './admin-users.state';

export const selectAdminUsersState = createFeatureSelector<AdminUsersState>(adminUsersFeatureKey);

export const selectAdminUsers = createSelector(selectAdminUsersState, (state) => state.users);
export const selectAdminUsersSelectedUser = createSelector(
  selectAdminUsersState,
  (state) => state.selectedUser
);
export const selectAdminUsersListLoading = createSelector(
  selectAdminUsersState,
  (state) => state.listLoading
);
export const selectAdminUsersDetailLoading = createSelector(
  selectAdminUsersState,
  (state) => state.detailLoading
);
export const selectAdminUsersMutationInProgress = createSelector(
  selectAdminUsersState,
  (state) => state.mutationInProgress
);
export const selectAdminUsersError = createSelector(selectAdminUsersState, (state) => state.error);
export const selectAdminUsersMutationError = createSelector(
  selectAdminUsersState,
  (state) => state.mutationError
);
export const selectAdminUsersTotalElements = createSelector(
  selectAdminUsersState,
  (state) => state.totalElements
);
