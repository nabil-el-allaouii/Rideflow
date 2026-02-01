package com.rideflow.demo.api.dto.audit;

import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.AuditStatus;
import java.time.Instant;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    String actorUserEmail,
    String actorUserFullName,
    AuditActorRole actorRole,
    AuditActionType actionType,
    AuditEntityType entityType,
    Long entityId,
    String payload,
    String ipAddress,
    String userAgent,
    AuditStatus status,
    Instant createdAt
) {
}
