package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.audit.AuditLogFilterRequest;
import com.rideflow.demo.api.dto.audit.AuditLogResponse;
import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> findAll(@ModelAttribute AuditLogFilterRequest filter) {
        return ResponseEntity.status(HttpStatus.OK).body(auditLogService.findAll(filter));
    }
}
