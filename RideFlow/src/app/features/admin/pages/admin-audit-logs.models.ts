export type AuditActionType =
  | 'LOGIN'
  | 'LOGOUT'
  | 'REGISTER'
  | 'SCOOTER_CREATE'
  | 'SCOOTER_UPDATE'
  | 'SCOOTER_STATUS_CHANGE'
  | 'SCOOTER_DELETE'
  | 'RENTAL_UNLOCK'
  | 'RENTAL_START'
  | 'RENTAL_CANCEL'
  | 'RENTAL_END'
  | 'RENTAL_FORCE_END'
  | 'PAYMENT_INITIATED'
  | 'PAYMENT_SUCCEEDED'
  | 'PAYMENT_FAILED'
  | 'PAYMENT_REFUNDED'
  | 'USER_STATUS_CHANGE';

export type AuditEntityType =
  | 'USER'
  | 'SCOOTER'
  | 'RENTAL'
  | 'PAYMENT'
  | 'RECEIPT'
  | 'PRICING_CONFIG'
  | 'AUTH';

export type AuditActorRole = 'CUSTOMER' | 'ADMIN' | 'SYSTEM';
export type AuditLogStatus = 'SUCCESS' | 'FAILED';

export interface AdminAuditLog {
  id: number;
  actorUserId?: number | null;
  actorUserEmail?: string | null;
  actorUserFullName?: string | null;
  actorRole: AuditActorRole;
  actionType: AuditActionType;
  entityType: AuditEntityType;
  entityId?: number | null;
  payload?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  status: AuditLogStatus;
  createdAt: string;
}

export interface AdminAuditLogFilters {
  query?: string | null;
  actorUserId?: number | null;
  actionType?: AuditActionType | null;
  entityType?: AuditEntityType | null;
  entityId?: number | null;
  status?: AuditLogStatus | null;
  fromDate?: string | null;
  toDate?: string | null;
  page?: number | null;
  size?: number | null;
}

export interface AdminAuditLogsPageResponse {
  content: AdminAuditLog[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
