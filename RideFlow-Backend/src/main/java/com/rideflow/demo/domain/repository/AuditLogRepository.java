package com.rideflow.demo.domain.repository;

import com.rideflow.demo.domain.model.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByActorUserId(Long actorUserId);

    @Override
    @EntityGraph(attributePaths = "actorUser")
    Page<AuditLog> findAll(Specification<AuditLog> specification, Pageable pageable);
}
