package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.audit.AuditLogFilterRequest;
import com.rideflow.demo.api.dto.audit.AuditLogResponse;
import com.rideflow.demo.api.dto.common.PageResponse;

public interface AuditLogService {
    PageResponse<AuditLogResponse> findAll(AuditLogFilterRequest filter);
}
