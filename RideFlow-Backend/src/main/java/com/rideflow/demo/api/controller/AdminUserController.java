package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.user.UserFilterRequest;
import com.rideflow.demo.api.dto.user.UserResponse;
import com.rideflow.demo.api.dto.user.UserStatusUpdateRequest;
import com.rideflow.demo.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> findAll(@ModelAttribute UserFilterRequest filter) {
        return ResponseEntity.ok(adminUserService.findAll(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(adminUserService.findById(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
        @PathVariable("id") Long userId,
        @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateStatus(userId, request));
    }
}
