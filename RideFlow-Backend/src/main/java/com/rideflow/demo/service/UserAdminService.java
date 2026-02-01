package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.common.PageResponse;
import com.rideflow.demo.api.dto.user.UserFilterRequest;
import com.rideflow.demo.api.dto.user.UserResponse;
import com.rideflow.demo.api.dto.user.UserStatusUpdateRequest;

public interface UserAdminService {
    PageResponse<UserResponse> findAllUsers(UserFilterRequest filter);
    UserResponse findUserById(Long userId);
    UserResponse updateUserStatus(Long userId, UserStatusUpdateRequest request);
}
