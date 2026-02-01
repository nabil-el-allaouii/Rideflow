package com.rideflow.demo.service.impl;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.user.UserFilterRequest;
import com.rideflow.demo.api.dto.user.UserResponse;
import com.rideflow.demo.api.dto.user.UserStatusUpdateRequest;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.api.exception.ResourceNotFoundException;
import com.rideflow.demo.domain.enums.AuditActionType;
import com.rideflow.demo.domain.enums.AuditEntityType;
import com.rideflow.demo.domain.enums.UserStatus;
import com.rideflow.demo.domain.model.User;
import com.rideflow.demo.domain.repository.UserRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.AdminUserService;
import com.rideflow.demo.service.AuditLogWriter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogWriter auditLogWriter;

    public AdminUserServiceImpl(
        UserRepository userRepository,
        AuthenticatedUserService authenticatedUserService,
        AuditLogWriter auditLogWriter
    ) {
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(UserFilterRequest filter) {
        Pageable pageable = PageRequest.of(
            normalizePage(filter.page()),
            normalizeSize(filter.size()),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<User> page = userRepository.findAll(buildSpecification(filter), pageable);

        return new PageResponse<>(
            page.getContent().stream().map(this::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        return toResponse(findUser(userId));
    }

    @Override
    public UserResponse updateStatus(Long userId, UserStatusUpdateRequest request) {
        if (request == null || request.status() == null) {
            throw new BusinessRuleException("User status is required.");
        }

        User user = findUser(userId);
        Long currentUserId = authenticatedUserService.getCurrentUserId();

        if (currentUserId.equals(user.id) && request.status() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("You cannot suspend or disable your own account.");
        }

        UserStatus previousStatus = user.status;
        user.status = request.status();
        User savedUser = userRepository.save(user);
        auditLogWriter.logSuccess(
            AuditActionType.USER_STATUS_CHANGE,
            AuditEntityType.USER,
            savedUser.id,
            payload(
                "email", savedUser.email,
                "previousStatus", previousStatus,
                "newStatus", savedUser.status
            )
        );
        return toResponse(savedUser);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.id,
            user.email,
            user.fullName,
            user.phoneNumber,
            user.preferredPaymentMethod,
            user.role,
            user.status,
            user.createdAt,
            user.lastLoginAt,
            user.updatedAt
        );
    }

    private Specification<User> buildSpecification(UserFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
            }

            String normalizedQuery = normalizeQuery(filter.query());
            if (normalizedQuery != null) {
                String pattern = "%" + normalizedQuery + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), pattern)
                ));
            }

            if (filter.role() != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), filter.role()));
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

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            payload.put(String.valueOf(values[index]), values[index + 1]);
        }
        return payload;
    }
}
