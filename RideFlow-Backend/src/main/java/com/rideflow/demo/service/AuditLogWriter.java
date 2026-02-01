package com.rideflow.demo.service;

import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.model.User;

public interface AuditLogWriter {
    void logSuccess(AuditActionType actionType, AuditEntityType entityType, Long entityId, Object payload);

    void logSuccess(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    );

    void logFailure(
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    );

    void logFailure(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    );

    void logFailureInNewTransaction(
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    );

    void logFailureInNewTransaction(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    );

    void logSystemSuccess(
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    );
}
