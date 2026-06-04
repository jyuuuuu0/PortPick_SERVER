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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserRepository userRepository;
    private final Path profileUploadDirectory;
    private final String profileUploadUrlPrefix;
    private final String defaultProfileImageUrl;

    public ProfileService(
            UserRepository userRepository,
            @Value("${app.profile.upload-dir:uploads/profiles}") String profileUploadDir,
            @Value("${app.profile.upload-url-prefix:/uploads/profiles}") String profileUploadUrlPrefix,
            @Value("${app.profile.default-image-url:/images/default-profile.svg}") String defaultProfileImageUrl
    ) {
        this.userRepository = userRepository;
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

        registerRollbackImageCleanup(profileDraft.newCustomImageUrlToDeleteOnFailure());

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
        registerImageDeletionAfterCommit(profileDraft.oldCustomImageUrlToDelete());
        return ProfileResponse.from(savedUser);
    }

    private void registerRollbackImageCleanup(String newImageUrl) {
        if (!StringUtils.hasText(newImageUrl) || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteProfileImageIfNeeded(newImageUrl);
                }
            }
        });
    }

    private void registerImageDeletionAfterCommit(String oldImageUrl) {
        if (!StringUtils.hasText(oldImageUrl)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteProfileImageIfNeeded(oldImageUrl);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteProfileImageIfNeeded(oldImageUrl);
            }
        });
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
            String uploadedUrl = storeProfileImage(profileImage);
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

    private String storeProfileImage(MultipartFile profileImage) {
        String originalFilename = profileImage.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        try {
            Files.createDirectories(profileUploadDirectory);
            String savedFilename = UUID.randomUUID() + "." + extension;
            Path target = profileUploadDirectory.resolve(savedFilename).normalize();

            try (InputStream inputStream = profileImage.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return profileUploadUrlPrefix + "/" + savedFilename;
        } catch (IOException exception) {
            throw new IllegalStateException("프로필 이미지를 저장하지 못했습니다.");
        }
    }

    private void deleteProfileImageIfNeeded(String imageUrlToDelete) {
        if (!StringUtils.hasText(imageUrlToDelete)) {
            return;
        }

        deleteStoredFile(imageUrlToDelete);
    }

    private void deleteStoredFile(String imageUrl) {
        if (!imageUrl.startsWith(profileUploadUrlPrefix + "/")) {
            return;
        }

        String filename = imageUrl.substring((profileUploadUrlPrefix + "/").length());
        if (!StringUtils.hasText(filename)) {
            return;
        }

        Path target = profileUploadDirectory.resolve(filename).normalize();
        if (!target.startsWith(profileUploadDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Ignore cleanup failures so profile updates are not rolled back by stale files.
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new IllegalArgumentException("이미지 파일 형식이 올바르지 않습니다.");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("프로필 이미지는 jpg, jpeg, png, gif, webp 파일만 업로드할 수 있습니다.");
        }

        return extension;
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
