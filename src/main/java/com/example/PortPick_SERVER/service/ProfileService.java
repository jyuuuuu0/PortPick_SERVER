package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.dto.ProfileResponse;
import com.example.PortPick_SERVER.dto.ProfileUpsertRequest;
import com.example.PortPick_SERVER.model.CareerRange;
import com.example.PortPick_SERVER.model.CareerType;
import com.example.PortPick_SERVER.model.JobRole;
import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Set;

@Service
public class ProfileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final Path profileUploadDirectory;
    private final String profileUploadUrlPrefix;
    private final String defaultProfileImageUrl;

    public ProfileService(
            UserRepository userRepository,
            FileStorageService fileStorageService,
            @Value("${app.profile.upload-dir:uploads/profiles}") String profileUploadDir,
            @Value("${app.profile.upload-url-prefix:/uploads/profiles}") String profileUploadUrlPrefix,
            @Value("${app.profile.default-image-url:/images/default-profile.svg}") String defaultProfileImageUrl
    ) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.profileUploadDirectory = Path.of(profileUploadDir).toAbsolutePath().normalize();
        this.profileUploadUrlPrefix = profileUploadUrlPrefix;
        this.defaultProfileImageUrl = defaultProfileImageUrl;
    }

    public ProfileResponse getMyProfile(String email) {
        return ProfileResponse.from(getUser(email));
    }

    @Transactional
    public ProfileResponse completeSignup(String email, ProfileUpsertRequest request, MultipartFile profileImage) {
        User user = getUser(email);
        if (user.isSignupCompleted()) {
            throw new IllegalArgumentException("이미 회원가입이 완료된 사용자입니다.");
        }

        return saveProfile(user, request, profileImage);
    }

    @Transactional
    public ProfileResponse updateMyProfile(String email, ProfileUpsertRequest request, MultipartFile profileImage) {
        User user = getUser(email);
        if (!user.isSignupCompleted()) {
            throw new IllegalArgumentException("회원가입을 먼저 완료해 주세요.");
        }

        return saveProfile(user, request, profileImage);
    }

    private ProfileResponse saveProfile(User user, ProfileUpsertRequest request, MultipartFile profileImage) {
        ProfileDraft profileDraft = buildProfileDraft(request, profileImage, user);

        fileStorageService.registerRollbackCleanup(
                profileDraft.newCustomImageUrlToDeleteOnFailure(),
                profileUploadDirectory, profileUploadUrlPrefix);

        user.completeSignup(
                profileDraft.name(),
                profileDraft.organizationName(),
                profileDraft.noOrganization(),
                profileDraft.jobRole(),
                profileDraft.careerType(),
                profileDraft.careerRange(),
                profileDraft.profileImageUrl(),
                profileDraft.customProfileImage()
        );

        User savedUser = userRepository.save(user);
        fileStorageService.registerDeletionAfterCommit(
                profileDraft.oldCustomImageUrlToDelete(),
                profileUploadDirectory, profileUploadUrlPrefix);
        return ProfileResponse.from(savedUser);
    }

    private User getUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Authenticated email is missing.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    private ProfileDraft buildProfileDraft(ProfileUpsertRequest request, MultipartFile profileImage, User user) {
        if (request == null) {
            throw new IllegalArgumentException("프로필 정보가 필요합니다.");
        }

        String name = requireText(request.getName(), "이름을 입력해 주세요.");
        boolean noOrganization = request.isNoOrganization();
        String organizationName = noOrganization
                ? null
                : requireText(request.getOrganizationName(), "소속 직장(학교)을 입력해 주세요.");
        JobRole jobRole = JobRole.from(request.getJobRole());
        CareerType careerType = CareerType.from(request.getCareerType());
        CareerRange careerRange = resolveCareerRange(careerType, request.getCareerRange());

        ImageDraft imageDraft = resolveProfileImage(profileImage, request.isDeleteProfileImage(), user);

        return new ProfileDraft(
                name,
                organizationName,
                noOrganization,
                jobRole,
                careerType,
                careerRange,
                imageDraft.profileImageUrl(),
                imageDraft.customProfileImage(),
                imageDraft.oldCustomImageUrlToDelete(),
                imageDraft.newCustomImageUrlToDeleteOnFailure()
        );
    }

    private CareerRange resolveCareerRange(CareerType careerType, String careerRangeValue) {
        if (careerType == CareerType.NEWCOMER) {
            return null;
        }

        return CareerRange.from(careerRangeValue);
    }

    private ImageDraft resolveProfileImage(MultipartFile profileImage, boolean deleteProfileImage, User user) {
        if (profileImage != null && !profileImage.isEmpty()) {
            String uploadedUrl = fileStorageService.store(
                    profileImage, profileUploadDirectory, profileUploadUrlPrefix,
                    ALLOWED_EXTENSIONS, "프로필 이미지를 저장하지 못했습니다.");
            String oldUrl = user.isCustomProfileImage() ? user.getProfileImageUrl() : null;
            return new ImageDraft(uploadedUrl, true, oldUrl, uploadedUrl);
        }

        if (deleteProfileImage) {
            String oldUrl = user.isCustomProfileImage() ? user.getProfileImageUrl() : null;
            return new ImageDraft(defaultProfileImageUrl, false, oldUrl, null);
        }

        if (StringUtils.hasText(user.getProfileImageUrl())) {
            return new ImageDraft(user.getProfileImageUrl(), user.isCustomProfileImage(), null, null);
        }

        return new ImageDraft(defaultProfileImageUrl, false, null, null);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private record ProfileDraft(
            String name,
            String organizationName,
            boolean noOrganization,
            JobRole jobRole,
            CareerType careerType,
            CareerRange careerRange,
            String profileImageUrl,
            boolean customProfileImage,
            String oldCustomImageUrlToDelete,
            String newCustomImageUrlToDeleteOnFailure
    ) {
    }

    private record ImageDraft(
            String profileImageUrl,
            boolean customProfileImage,
            String oldCustomImageUrlToDelete,
            String newCustomImageUrlToDeleteOnFailure
    ) {
    }
}
