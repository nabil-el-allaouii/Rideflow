package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.user.UserFilterRequest;
import com.rideflow.demo.api.dto.user.UserResponse;
import com.rideflow.demo.api.dto.user.UserStatusUpdateRequest;

public interface AdminUserService {
    PageResponse<UserResponse> findAll(UserFilterRequest filter);
    UserResponse findById(Long userId);
    UserResponse updateStatus(Long userId, UserStatusUpdateRequest request);
}
