package com.rideflow.demo.service;

import com.rideflow.demo.api.dto.user.ProfileUpdateRequest;
import com.rideflow.demo.api.dto.user.UserResponse;

public interface UserProfileService {
    UserResponse getCurrentProfile();
    UserResponse updateCurrentProfile(ProfileUpdateRequest request);
}
