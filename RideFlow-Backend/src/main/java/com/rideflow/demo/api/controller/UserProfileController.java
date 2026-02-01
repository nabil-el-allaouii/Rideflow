package com.rideflow.demo.api.controller;

import com.rideflow.demo.api.dto.user.ProfileUpdateRequest;
import com.rideflow.demo.api.dto.user.UserResponse;
import com.rideflow.demo.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentProfile() {
        return ResponseEntity.ok(userProfileService.getCurrentProfile());
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateCurrentProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateCurrentProfile(request));
    }
}
