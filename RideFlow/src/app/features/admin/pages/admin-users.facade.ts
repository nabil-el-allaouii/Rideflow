import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { UserStatus } from '../../auth/store/auth.models';
import { adminUsersActions } from './admin-users.actions';
import { AdminUsersFilters } from './admin-users.models';
import {
  selectAdminUsers,
  selectAdminUsersDetailLoading,
  selectAdminUsersError,
  selectAdminUsersListLoading,
  selectAdminUsersMutationError,
  selectAdminUsersMutationInProgress,
  selectAdminUsersSelectedUser,
  selectAdminUsersTotalElements
} from './admin-users.selectors';

@Injectable({ providedIn: 'root' })
export class AdminUsersFacade {
  private readonly store = inject(Store);

  readonly users$ = this.store.select(selectAdminUsers);
  readonly selectedUser$ = this.store.select(selectAdminUsersSelectedUser);
  readonly listLoading$ = this.store.select(selectAdminUsersListLoading);
  readonly detailLoading$ = this.store.select(selectAdminUsersDetailLoading);
  readonly mutationInProgress$ = this.store.select(selectAdminUsersMutationInProgress);
  readonly error$ = this.store.select(selectAdminUsersError);
  readonly mutationError$ = this.store.select(selectAdminUsersMutationError);
  readonly totalElements$ = this.store.select(selectAdminUsersTotalElements);

  loadList(filters: AdminUsersFilters): void {
    this.store.dispatch(adminUsersActions.loadListRequested({ filters }));
  }

  loadDetail(userId: number): void {
    this.store.dispatch(adminUsersActions.loadDetailRequested({ userId }));
  }

  updateStatus(userId: number, status: UserStatus): void {
    this.store.dispatch(adminUsersActions.updateStatusRequested({ userId, status }));
  }

  clearMutationError(): void {
    this.store.dispatch(adminUsersActions.clearMutationError());
  }
}
