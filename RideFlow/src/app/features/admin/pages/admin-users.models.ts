export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  role: import('../../auth/store/auth.models').UserRole;
  status: import('../../auth/store/auth.models').UserStatus;
  createdAt: string;
  lastLoginAt?: string | null;
  updatedAt: string;
}

export interface AdminUsersFilters {
  query?: string;
  role?: import('../../auth/store/auth.models').UserRole | null;
  status?: import('../../auth/store/auth.models').UserStatus | null;
  page?: number | null;
  size?: number | null;
}

export interface AdminUsersPageResponse {
  content: AdminUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminUserStatusUpdateRequest {
  status: import('../../auth/store/auth.models').UserStatus;
}
