package com.resume.service;

import com.resume.model.dto.ProfileRequest;
import com.resume.model.dto.ProfileResponse;
import com.resume.model.entity.UserProfile;
import com.resume.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;

    public ProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * 获取用户画像，不存在则返回空画像(profileCompleted=false)
     */
    public ProfileResponse getProfile(Long userId) {
        Optional<UserProfile> optional = userProfileRepository.findByUserId(userId);
        if (optional.isPresent()) {
            return toResponse(optional.get());
        }
        // 用户未创建画像，返回空对象
        return ProfileResponse.builder()
                .profileCompleted(false)
                .build();
    }

    /**
     * 更新用户画像，存在则更新，不存在则创建
     */
    public ProfileResponse updateProfile(Long userId, ProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUserId(userId);
                    return newProfile;
                });

        // 更新字段
        profile.setWorkYears(request.getWorkYears());
        profile.setTechDirection(request.getTechDirection());
        profile.setTargetPosition(request.getTargetPosition());
        profile.setTargetIndustry(request.getTargetIndustry());
        profile.setSalaryRange(request.getSalaryRange());
        profile.setEducation(request.getEducation());
        profile.setCoreSkills(request.getCoreSkills());

        UserProfile saved = userProfileRepository.save(profile);
        return toResponse(saved);
    }

    private ProfileResponse toResponse(UserProfile profile) {
        boolean completed = StringUtils.hasText(profile.getWorkYears())
                && StringUtils.hasText(profile.getTargetPosition());

        return ProfileResponse.builder()
                .id(profile.getId())
                .workYears(profile.getWorkYears())
                .techDirection(profile.getTechDirection())
                .targetPosition(profile.getTargetPosition())
                .targetIndustry(profile.getTargetIndustry())
                .salaryRange(profile.getSalaryRange())
                .education(profile.getEducation())
                .coreSkills(profile.getCoreSkills())
                .updatedAt(profile.getUpdatedAt())
                .profileCompleted(completed)
                .build();
    }
}
