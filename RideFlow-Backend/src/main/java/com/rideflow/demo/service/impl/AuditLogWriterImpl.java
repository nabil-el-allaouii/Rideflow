package com.rideflow.demo.service.impl;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.AuditStatus;
import com.rideflow.demo.domain.enums.UserRole;
import com.rideflow.demo.domain.model.AuditLog;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.AuditLogRepository;
import com.rideflow.demo.security.RideFlowUserPrincipal;
import com.rideflow.demo.service.AuditLogWriter;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogWriterImpl implements AuditLogWriter {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    public AuditLogWriterImpl(
        AuditLogRepository auditLogRepository,
        EntityManager entityManager
    ) {
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void logSuccess(AuditActionType actionType, AuditEntityType entityType, Long entityId, Object payload) {
        write(null, null, actionType, entityType, entityId, payload, AuditStatus.SUCCESS);
    }

    @Override
    @Transactional
    public void logSuccess(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    ) {
        write(actorUser, actorRole, actionType, entityType, entityId, payload, AuditStatus.SUCCESS);
    }

    @Override
    @Transactional
    public void logFailure(
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    ) {
        write(null, null, actionType, entityType, entityId, payload, AuditStatus.FAILED);
    }

    @Override
    @Transactional
    public void logFailure(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    ) {
        write(actorUser, actorRole, actionType, entityType, entityId, payload, AuditStatus.FAILED);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailureInNewTransaction(
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    ) {
        write(null, null, actionType, entityType, entityId, payload, AuditStatus.FAILED);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailureInNewTransaction(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    ) {
        write(actorUser, actorRole, actionType, entityType, entityId, payload, AuditStatus.FAILED);
    }

    @Override
    @Transactional
    public void logSystemSuccess(
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload
    ) {
        write(null, AuditActorRole.SYSTEM, actionType, entityType, entityId, payload, AuditStatus.SUCCESS);
    }

    private void write(
        User actorUser,
        AuditActorRole actorRole,
        AuditActionType actionType,
        AuditEntityType entityType,
        Long entityId,
        Object payload,
        AuditStatus status
    ) {
        AuditLog auditLog = new AuditLog();

        Long actorUserId = actorUser == null ? resolveCurrentUserId() : actorUser.id;
        AuditActorRole resolvedActorRole = actorRole != null
            ? actorRole
            : resolveActorRole(actorUser == null ? null : actorUser.role);

        if (resolvedActorRole == AuditActorRole.ADMIN) {
            return;
        }

        if (actorUserId != null) {
            auditLog.actorUser = entityManager.getReference(User.class, actorUserId);
        }

        auditLog.actorRole = resolvedActorRole;
        auditLog.actionType = actionType;
        auditLog.entityType = entityType;
        auditLog.entityId = entityId;
        auditLog.payload = serializePayload(payload);
        auditLog.status = status;
        auditLog.ipAddress = resolveIpAddress();
        auditLog.userAgent = resolveUserAgent();

        auditLogRepository.save(auditLog);
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof RideFlowUserPrincipal principal)) {
            return null;
        }
        return principal.getUserId();
    }

    private AuditActorRole resolveActorRole(UserRole role) {
        if (role == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof RideFlowUserPrincipal principal) {
                role = principal.getRole();
            }
        }

        if (role == null) {
            return AuditActorRole.SYSTEM;
        }

        return role == UserRole.ADMIN ? AuditActorRole.ADMIN : AuditActorRole.CUSTOMER;
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }

        if (payload instanceof String value) {
            return value;
        }

        try {
            return toJsonValue(payload);
        } catch (RuntimeException exception) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("serializationError", exception.getMessage());
            fallback.put("value", String.valueOf(payload));
            return toJsonValue(fallback);
        }
    }

    private String resolveIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        if (value instanceof Enum<?> enumValue) {
            return "\"" + escapeJson(enumValue.name()) + "\"";
        }

        if (value instanceof CharSequence || value instanceof Character) {
            return "\"" + escapeJson(String.valueOf(value)) + "\"";
        }

        if (value instanceof Map<?, ?> mapValue) {
            return mapValue.entrySet().stream()
                .map(entry -> "\"" + escapeJson(String.valueOf(entry.getKey())) + "\":" + toJsonValue(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
        }

        if (value instanceof Collection<?> collectionValue) {
            return collectionValue.stream()
                .map(this::toJsonValue)
                .collect(Collectors.joining(",", "[", "]"));
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            StringBuilder builder = new StringBuilder("[");
            for (int index = 0; index < length; index++) {
                if (index > 0) {
                    builder.append(',');
                }
                builder.append(toJsonValue(Array.get(value, index)));
            }
            return builder.append(']').toString();
        }

        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }
}
