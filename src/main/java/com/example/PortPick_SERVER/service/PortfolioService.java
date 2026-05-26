package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.dto.PortfolioCreateRequest;
import com.example.PortPick_SERVER.dto.PortfolioDetailResponse;
import com.example.PortPick_SERVER.dto.PortfolioUpdateRequest;
import com.example.PortPick_SERVER.model.Portfolio;
import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.PortfolioRepository;
import com.example.PortPick_SERVER.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class PortfolioService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "ppt", "pptx", "doc", "docx", "xls", "xlsx",
            "jpg", "jpeg", "png", "gif", "webp", "zip"
    );

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final Path portfolioUploadDirectory;
    private final String portfolioUploadUrlPrefix;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            @Value("${app.portfolio.upload-dir:uploads/portfolios}") String portfolioUploadDir,
            @Value("${app.portfolio.upload-url-prefix:/uploads/portfolios}") String portfolioUploadUrlPrefix
    ) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.portfolioUploadDirectory = Path.of(portfolioUploadDir).toAbsolutePath().normalize();
        this.portfolioUploadUrlPrefix = portfolioUploadUrlPrefix;
    }

    @Transactional
    public PortfolioDetailResponse createPortfolio(String email, PortfolioCreateRequest request, MultipartFile file) {
        User user = getUser(email);
        if (!user.isSignupCompleted()) {
            throw new IllegalStateException("회원가입을 먼저 완료해 주세요.");
        }
        if (request == null) {
            throw new IllegalArgumentException("포트폴리오 정보가 필요합니다.");
        }

        String title = requireText(request.getTitle(), "포트폴리오 제목을 입력해 주세요.");
        String description = requireText(request.getDescription(), "포트폴리오 설명을 입력해 주세요.");
        String embedLink = normalizeOptionalText(request.getEmbedLink());

        validateAttachmentInput(embedLink, file);
        validateEmbedLink(embedLink);

        StoredFile storedFile = storePortfolioFile(file);
        Portfolio portfolio = new Portfolio(
                user,
                title,
                description,
                embedLink,
                storedFile != null ? storedFile.fileUrl() : null,
                storedFile != null ? storedFile.originalFileName() : null
        );

        try {
            return PortfolioDetailResponse.from(portfolioRepository.save(portfolio));
        } catch (RuntimeException exception) {
            deleteStoredFileIfNeeded(storedFile != null ? storedFile.fileUrl() : null);
            throw exception;
        }
    }

    @Transactional
    public PortfolioDetailResponse updatePortfolio(String email, Long portfolioId, PortfolioUpdateRequest request) {
        User user = getUser(email);
        Portfolio portfolio = getPortfolio(portfolioId);
        validateOwner(user, portfolio);

        if (request == null) {
            throw new IllegalArgumentException("포트폴리오 수정 정보가 필요합니다.");
        }

        String title = requireText(request.getTitle(), "포트폴리오 제목을 입력해 주세요.");
        String description = requireText(request.getDescription(), "포트폴리오 설명을 입력해 주세요.");

        portfolio.updateText(title, description);
        return PortfolioDetailResponse.from(portfolio);
    }

    @Transactional
    public void deletePortfolio(String email, Long portfolioId) {
        User user = getUser(email);
        Portfolio portfolio = getPortfolio(portfolioId);
        validateOwner(user, portfolio);

        String fileUrl = portfolio.getFileUrl();
        portfolioRepository.delete(portfolio);
        deleteStoredFileIfNeeded(fileUrl);
    }

    @Transactional(readOnly = true)
    public PortfolioDetailResponse getPortfolioDetail(Long portfolioId) {
        return PortfolioDetailResponse.from(getPortfolio(portfolioId));
    }

    private User getUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Authenticated email is missing.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    private Portfolio getPortfolio(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
    }

    private void validateOwner(User user, Portfolio portfolio) {
        if (!portfolio.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인이 등록한 포트폴리오만 수정하거나 삭제할 수 있습니다.");
        }
    }

    private void validateAttachmentInput(String embedLink, MultipartFile file) {
        boolean hasEmbedLink = StringUtils.hasText(embedLink);
        boolean hasFile = file != null && !file.isEmpty();

        if (!hasEmbedLink && !hasFile) {
            throw new IllegalArgumentException("임베드 링크 또는 포트폴리오 파일을 하나 이상 등록해 주세요.");
        }
    }

    private void validateEmbedLink(String embedLink) {
        if (!StringUtils.hasText(embedLink)) {
            return;
        }

        try {
            URI uri = new URI(embedLink);
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("유효한 임베드 링크를 입력해 주세요.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("유효한 임베드 링크를 입력해 주세요.");
        }
    }

    private StoredFile storePortfolioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = requireText(file.getOriginalFilename(), "업로드 파일명이 올바르지 않습니다.");
        String extension = extractExtension(originalFilename);

        try {
            Files.createDirectories(portfolioUploadDirectory);
            String savedFilename = UUID.randomUUID() + "." + extension;
            Path target = portfolioUploadDirectory.resolve(savedFilename).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredFile(portfolioUploadUrlPrefix + "/" + savedFilename, originalFilename);
        } catch (IOException exception) {
            throw new IllegalStateException("포트폴리오 파일을 저장하지 못했습니다.");
        }
    }

    private void deleteStoredFileIfNeeded(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        if (!fileUrl.startsWith(portfolioUploadUrlPrefix + "/")) {
            return;
        }

        String filename = fileUrl.substring((portfolioUploadUrlPrefix + "/").length());
        if (!StringUtils.hasText(filename)) {
            return;
        }

        Path target = portfolioUploadDirectory.resolve(filename).normalize();
        if (!target.startsWith(portfolioUploadDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Ignore cleanup failures after delete.
        }
    }

    private String extractExtension(String filename) {
        if (!filename.contains(".")) {
            throw new IllegalArgumentException("업로드 파일 형식이 올바르지 않습니다.");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 포트폴리오 파일 형식입니다.");
        }

        return extension;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record StoredFile(
            String fileUrl,
            String originalFileName
    ) {
    }
}
