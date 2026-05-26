package com.example.PortPick_SERVER.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    private String provider;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean signupCompleted;

    @Column
    private String organizationName;

    @Column(nullable = false)
    private boolean noOrganization;

    @Enumerated(EnumType.STRING)
    @Column
    private JobRole jobRole;

    @Enumerated(EnumType.STRING)
    @Column
    private CareerType careerType;

    @Enumerated(EnumType.STRING)
    @Column
    private CareerRange careerRange;

    @Column(nullable = false)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean customProfileImage;

    @Builder
    public User(
            String email,
            String name,
            String provider,
            String role,
            boolean signupCompleted,
            String organizationName,
            boolean noOrganization,
            JobRole jobRole,
            CareerType careerType,
            CareerRange careerRange,
            String profileImageUrl,
            boolean customProfileImage
    ) {
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.role = role != null ? role : "ROLE_USER";
        this.signupCompleted = signupCompleted;
        this.organizationName = organizationName;
        this.noOrganization = noOrganization;
        this.jobRole = jobRole;
        this.careerType = careerType;
        this.careerRange = careerRange;
        this.profileImageUrl = profileImageUrl;
        this.customProfileImage = customProfileImage;
    }

    public User updateOAuthName(String name) {
        this.name = name;
        return this;
    }

    public void completeSignup(
            String name,
            String organizationName,
            boolean noOrganization,
            JobRole jobRole,
            CareerType careerType,
            CareerRange careerRange,
            String profileImageUrl,
            boolean customProfileImage
    ) {
        this.name = name;
        this.organizationName = organizationName;
        this.noOrganization = noOrganization;
        this.jobRole = jobRole;
        this.careerType = careerType;
        this.careerRange = careerRange;
        this.profileImageUrl = profileImageUrl;
        this.customProfileImage = customProfileImage;
        this.signupCompleted = true;
    }

    public void updateProfileImage(String profileImageUrl, boolean customProfileImage) {
        this.profileImageUrl = profileImageUrl;
        this.customProfileImage = customProfileImage;
    }
}
