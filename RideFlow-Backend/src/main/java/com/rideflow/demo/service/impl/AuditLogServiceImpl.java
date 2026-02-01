package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.audit.AuditLogFilterRequest;
import com.rideflow.demo.api.dto.audit.AuditLogResponse;
import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.domain.enums.AuditActorRole;
import com.rideflow.demo.domain.model.AuditLog;
import com.rideflow.demo.domain.repository.AuditLogRepository;
import com.rideflow.demo.service.AuditLogService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public PageResponse<AuditLogResponse> findAll(AuditLogFilterRequest filter) {
        Pageable pageable = PageRequest.of(
            normalizePage(filter == null ? null : filter.page()),
            normalizeSize(filter == null ? null : filter.size()),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLog> page = auditLogRepository.findAll(buildSpecification(filter), pageable);

        return new PageResponse<>(
            page.getContent().stream().map(this::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.id,
            auditLog.actorUser == null ? null : auditLog.actorUser.id,
            auditLog.actorUser == null ? null : auditLog.actorUser.email,
            auditLog.actorUser == null ? null : auditLog.actorUser.fullName,
            auditLog.actorRole,
            auditLog.actionType,
            auditLog.entityType,
            auditLog.entityId,
            auditLog.payload,
            auditLog.ipAddress,
            auditLog.userAgent,
            auditLog.status,
            auditLog.createdAt
        );
    }

    private Specification<AuditLog> buildSpecification(AuditLogFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            root.join("actorUser", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
            }

            String normalizedQuery = normalizeQuery(filter.query());
            if (normalizedQuery != null) {
                String pattern = "%" + normalizedQuery + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("payload")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("actorUser", JoinType.LEFT).get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("actorUser", JoinType.LEFT).get("fullName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("ipAddress")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("userAgent")), pattern)
                ));
            }

            if (filter.actorUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUser").get("id"), filter.actorUserId()));
            }

            if (filter.actionType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), filter.actionType()));
            }

            if (filter.entityType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), filter.entityType()));
            }

            if (filter.entityId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), filter.entityId()));
            }

            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
            }

            if (filter.fromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.fromDate()));
            }

            if (filter.toDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.toDate()));
            }

            predicates.add(criteriaBuilder.notEqual(root.get("actorRole"), AuditActorRole.ADMIN));

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
