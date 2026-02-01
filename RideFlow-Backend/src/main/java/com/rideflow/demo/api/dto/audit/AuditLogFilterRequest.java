package com.rideflow.demo.api.dto.audit;

import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.AuditStatus;
import java.time.Instant;

public record AuditLogFilterRequest(
    String query,
    Long actorUserId,
    AuditActionType actionType,
    AuditEntityType entityType,
    Long entityId,
    AuditStatus status,
    Instant fromDate,
    Instant toDate,
    Integer page,
    Integer size
) {
}
