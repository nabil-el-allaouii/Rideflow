import { createReducer, on } from '@ngrx/store';
import { adminUsersActions } from './admin-users.actions';
import { AdminUser } from './admin-users.models';
import { AdminUsersState, initialAdminUsersState } from './admin-users.state';

export const adminUsersFeatureKey = 'adminUsers';

export const adminUsersReducer = createReducer<AdminUsersState>(
  initialAdminUsersState,
  on(adminUsersActions.loadListRequested, (state, { filters }) => ({
    ...state,
    listLoading: true,
    error: null,
    filters
  })),
  on(adminUsersActions.loadListSucceeded, (state, { response, filters }) => ({
    ...state,
    users: response.content,
    listLoading: false,
    error: null,
    filters,
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
    selectedUser: syncSelectedUser(state.selectedUser, response.content)
  })),
  on(adminUsersActions.loadListFailed, (state, { error }) => ({
    ...state,
    listLoading: false,
    error
  })),
  on(adminUsersActions.loadDetailRequested, (state) => ({
    ...state,
    detailLoading: true,
    error: null
  })),
  on(adminUsersActions.loadDetailSucceeded, (state, { user }) => ({
    ...state,
    detailLoading: false,
    selectedUser: user,
    users: upsertUser(state.users, user)
  })),
  on(adminUsersActions.loadDetailFailed, (state, { error }) => ({
    ...state,
    detailLoading: false,
    error
  })),
  on(adminUsersActions.updateStatusRequested, (state) => ({
    ...state,
    mutationInProgress: true,
    mutationError: null
  })),
  on(adminUsersActions.updateStatusSucceeded, (state, { user }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    selectedUser: state.selectedUser?.id === user.id ? user : state.selectedUser,
    users: upsertUser(state.users, user)
  })),
  on(adminUsersActions.updateStatusFailed, (state, { error }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: error
  })),
  on(adminUsersActions.clearMutationError, (state) => ({
    ...state,
    mutationError: null
  }))
);

function syncSelectedUser(selectedUser: AdminUser | null, users: AdminUser[]): AdminUser | null {
  if (!selectedUser) {
    return null;
  }

  return users.find((user) => user.id === selectedUser.id) ?? null;
}

function upsertUser(users: AdminUser[], user: AdminUser): AdminUser[] {
  return users.map((currentUser) => currentUser.id === user.id ? user : currentUser);
}
