package com.resume.controller;

import com.resume.model.dto.ApiResponse;
import com.resume.model.dto.ProfileRequest;
import com.resume.model.dto.ProfileResponse;
import com.resume.service.ProfileService;
import com.resume.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 获取当前用户画像
     */
    @GetMapping
    public ApiResponse<ProfileResponse> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileResponse response = profileService.getProfile(userId);
        return ApiResponse.success(response);
    }

    /**
     * 更新当前用户画像
     */
    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(@RequestBody ProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ApiResponse.success("画像更新成功", response);
    }
}
