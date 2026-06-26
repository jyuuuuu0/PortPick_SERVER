package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.dto.PortfolioCreateRequest;
import com.example.PortPick_SERVER.dto.PortfolioDetailResponse;
import com.example.PortPick_SERVER.dto.PortfolioLikeResponse;
import com.example.PortPick_SERVER.dto.PortfolioSummaryResponse;
import com.example.PortPick_SERVER.dto.PortfolioUpdateRequest;
import com.example.PortPick_SERVER.model.CareerRange;
import com.example.PortPick_SERVER.model.CareerType;
import com.example.PortPick_SERVER.model.JobRole;
import com.example.PortPick_SERVER.model.Portfolio;
import com.example.PortPick_SERVER.model.PortfolioLike;
import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.CommentRepository;
import com.example.PortPick_SERVER.repository.PortfolioLikeRepository;
import com.example.PortPick_SERVER.repository.PortfolioRepository;
import com.example.PortPick_SERVER.repository.ReplyRepository;
import com.example.PortPick_SERVER.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "ppt", "pptx", "doc", "docx", "xls", "xlsx",
            "jpg", "jpeg", "png", "gif", "webp", "zip"
    );

    private final PortfolioRepository portfolioRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final Path portfolioUploadDirectory;
    private final String portfolioUploadUrlPrefix;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            PortfolioLikeRepository portfolioLikeRepository,
            CommentRepository commentRepository,
            ReplyRepository replyRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            @Value("${app.portfolio.upload-dir:uploads/portfolios}") String portfolioUploadDir,
            @Value("${app.portfolio.upload-url-prefix:/uploads/portfolios}") String portfolioUploadUrlPrefix
    ) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioLikeRepository = portfolioLikeRepository;
        this.commentRepository = commentRepository;
        this.replyRepository = replyRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.portfolioUploadDirectory = Path.of(portfolioUploadDir).toAbsolutePath().normalize();
        this.portfolioUploadUrlPrefix = portfolioUploadUrlPrefix;
    }

    @Transactional(readOnly = true)
    public List<PortfolioSummaryResponse> getPortfolios(
            String email,
            String jobRoleValue,
            String careerTypeValue,
            String careerRangeValue
    ) {
        JobRole jobRole = parseJobRole(jobRoleValue);
        CareerType careerType = parseCareerType(careerTypeValue);
        CareerRange careerRange = parseCareerRange(careerRangeValue);

        List<Portfolio> portfolios = portfolioRepository.findAllByFilters(jobRole, careerType, careerRange);
        List<Long> portfolioIds = portfolios.stream().map(Portfolio::getId).toList();

        Map<Long, Long> likeCounts = getLikeCounts(portfolioIds);
        Set<Long> likedPortfolioIds = getLikedPortfolioIds(email, portfolioIds);

        return portfolios.stream()
                .map(portfolio -> PortfolioSummaryResponse.from(
                        portfolio,
                        likeCounts.getOrDefault(portfolio.getId(), 0L),
                        likedPortfolioIds.contains(portfolio.getId())
                ))
                .toList();
    }

    @Transactional
    public PortfolioDetailResponse createPortfolio(String email, PortfolioCreateRequest request, MultipartFile file) {
        User user = getUser(email);
        if (!user.isSignupCompleted()) {
            throw new AccessDeniedException("회원가입을 먼저 완료해 주세요.");
        }
        if (request == null) {
            throw new IllegalArgumentException("포트폴리오 정보가 필요합니다.");
        }

        String title = requireText(request.getTitle(), "포트폴리오 제목을 입력해 주세요.");
        String description = requireText(request.getDescription(), "포트폴리오 설명을 입력해 주세요.");
        validateLength(title, 255, "포트폴리오 제목은 255자 이하로 입력해 주세요.");
        validateLength(description, 5000, "포트폴리오 설명은 5000자 이하로 입력해 주세요.");
        String embedLink = normalizeOptionalText(request.getEmbedLink());

        validateAttachmentInput(embedLink, file);
        validateEmbedLink(embedLink);

        String fileUrl = null;
        String originalFileName = null;
        if (file != null && !file.isEmpty()) {
            originalFileName = requireText(file.getOriginalFilename(), "업로드 파일명이 올바르지 않습니다.");
            fileUrl = fileStorageService.store(file, portfolioUploadDirectory, portfolioUploadUrlPrefix,
                    ALLOWED_EXTENSIONS, "포트폴리오 파일을 저장하지 못했습니다.");
            fileStorageService.registerRollbackCleanup(fileUrl, portfolioUploadDirectory, portfolioUploadUrlPrefix);
        }

        Portfolio portfolio = new Portfolio(user, title, description, embedLink, fileUrl, originalFileName);
        return PortfolioDetailResponse.from(portfolioRepository.save(portfolio), 0L, false);
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
        validateLength(title, 255, "포트폴리오 제목은 255자 이하로 입력해 주세요.");
        validateLength(description, 5000, "포트폴리오 설명은 5000자 이하로 입력해 주세요.");

        portfolio.updateText(title, description);
        return PortfolioDetailResponse.from(
                portfolio,
                portfolioLikeRepository.countByPortfolioId(portfolioId),
                portfolioLikeRepository.existsByPortfolioIdAndUserId(portfolioId, user.getId())
        );
    }

    @Transactional
    public void deletePortfolio(String email, Long portfolioId) {
        User user = getUser(email);
        Portfolio portfolio = getPortfolio(portfolioId);
        validateOwner(user, portfolio);

        String fileUrl = portfolio.getFileUrl();
        replyRepository.deleteByPortfolioId(portfolioId);
        commentRepository.deleteByPortfolioId(portfolioId);
        portfolioRepository.delete(portfolio);
        fileStorageService.registerDeletionAfterCommit(fileUrl, portfolioUploadDirectory, portfolioUploadUrlPrefix);
    }

    @Transactional(readOnly = true)
    public PortfolioDetailResponse getPortfolioDetail(String email, Long portfolioId) {
        Portfolio portfolio = getPortfolio(portfolioId);
        User currentUser = findUser(email);
        long likeCount = portfolioLikeRepository.countByPortfolioId(portfolioId);
        boolean liked = currentUser != null
                && portfolioLikeRepository.existsByPortfolioIdAndUserId(portfolioId, currentUser.getId());

        return PortfolioDetailResponse.from(portfolio, likeCount, liked);
    }

    @Transactional
    public PortfolioLikeResponse likePortfolio(String email, Long portfolioId) {
        User user = getUser(email);
        Portfolio portfolio = getPortfolio(portfolioId);

        if (!portfolioLikeRepository.existsByPortfolioIdAndUserId(portfolioId, user.getId())) {
            try {
                portfolioLikeRepository.saveAndFlush(new PortfolioLike(portfolio, user));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 요청으로 인한 중복 INSERT — 이미 좋아요 상태이므로 정상 처리
            }
        }

        return buildLikeResponse(portfolioId, true);
    }

    @Transactional
    public PortfolioLikeResponse unlikePortfolio(String email, Long portfolioId) {
        User user = getUser(email);
        getPortfolio(portfolioId);

        portfolioLikeRepository.findByPortfolioIdAndUserId(portfolioId, user.getId())
                .ifPresent(portfolioLikeRepository::delete);

        return buildLikeResponse(portfolioId, false);
    }

    @Transactional(readOnly = true)
    public PortfolioLikeResponse getPortfolioLikeStatus(String email, Long portfolioId) {
        getPortfolio(portfolioId);
        User currentUser = findUser(email);
        boolean liked = currentUser != null
                && portfolioLikeRepository.existsByPortfolioIdAndUserId(portfolioId, currentUser.getId());

        return new PortfolioLikeResponse(
                portfolioId,
                portfolioLikeRepository.countByPortfolioId(portfolioId),
                liked
        );
    }

    private PortfolioLikeResponse buildLikeResponse(Long portfolioId, boolean liked) {
        return new PortfolioLikeResponse(
                portfolioId,
                portfolioLikeRepository.countByPortfolioId(portfolioId),
                liked
        );
    }

    private User getUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Authenticated email is missing.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    private User findUser(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private Portfolio getPortfolio(Long portfolioId) {
        return portfolioRepository.findWithUserById(portfolioId)
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

    private Map<Long, Long> getLikeCounts(Collection<Long> portfolioIds) {
        if (portfolioIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return portfolioLikeRepository.countByPortfolioIds(portfolioIds).stream()
                .collect(Collectors.toMap(
                        PortfolioLikeRepository.PortfolioLikeCountProjection::getPortfolioId,
                        PortfolioLikeRepository.PortfolioLikeCountProjection::getLikeCount
                ));
    }

    private Set<Long> getLikedPortfolioIds(String email, Collection<Long> portfolioIds) {
        if (!StringUtils.hasText(email) || portfolioIds.isEmpty()) {
            return Collections.emptySet();
        }

        User user = findUser(email);
        if (user == null) {
            return Collections.emptySet();
        }

        return portfolioLikeRepository.findLikedPortfolioIdsByUserIdAndPortfolioIds(user.getId(), portfolioIds).stream()
                .collect(Collectors.toSet());
    }

    private JobRole parseJobRole(String value) {
        return StringUtils.hasText(value) ? JobRole.from(value) : null;
    }

    private CareerType parseCareerType(String value) {
        return StringUtils.hasText(value) ? CareerType.from(value) : null;
    }

    private CareerRange parseCareerRange(String value) {
        return StringUtils.hasText(value) ? CareerRange.from(value) : null;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
