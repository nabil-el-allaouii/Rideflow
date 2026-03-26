import { AdminUser, AdminUsersFilters } from './admin-users.models';

export interface AdminUsersState {
  users: AdminUser[];
  selectedUser: AdminUser | null;
  listLoading: boolean;
  detailLoading: boolean;
  mutationInProgress: boolean;
  error: string | null;
  mutationError: string | null;
  filters: AdminUsersFilters;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const defaultAdminUsersFilters: AdminUsersFilters = {
  page: 0,
  size: 50
};

export const initialAdminUsersState: AdminUsersState = {
  users: [],
  selectedUser: null,
  listLoading: false,
  detailLoading: false,
  mutationInProgress: false,
  error: null,
  mutationError: null,
  filters: defaultAdminUsersFilters,
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0
};
