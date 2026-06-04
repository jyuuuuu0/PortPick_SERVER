package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.User;

public record ProfileResponse(
        String email,
        String name,
        String organizationName,
        boolean noOrganization,
        String jobRole,
        String careerType,
        String careerRange,
        String profileImageUrl,
        boolean customProfileImage,
        boolean signupCompleted
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getEmail(),
                user.getName(),
                user.isNoOrganization() ? null : user.getOrganizationName(),
                user.isNoOrganization(),
                user.getJobRole() != null ? user.getJobRole().getLabel() : null,
                user.getCareerType() != null ? user.getCareerType().getLabel() : null,
                user.getCareerRange() != null ? user.getCareerRange().getLabel() : null,
                user.getProfileImageUrl(),
                user.isCustomProfileImage(),
                user.isSignupCompleted()
        );
    }
}
