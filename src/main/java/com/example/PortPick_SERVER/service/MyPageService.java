package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.dto.MyPageResponse;
import com.example.PortPick_SERVER.dto.PortfolioSummaryResponse;
import com.example.PortPick_SERVER.model.Portfolio;
import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.CommentRepository;
import com.example.PortPick_SERVER.repository.PortfolioLikeRepository;
import com.example.PortPick_SERVER.repository.PortfolioRepository;
import com.example.PortPick_SERVER.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MyPageService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioLikeRepository portfolioLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public MyPageService(
            PortfolioRepository portfolioRepository,
            PortfolioLikeRepository portfolioLikeRepository,
            CommentRepository commentRepository,
            UserRepository userRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioLikeRepository = portfolioLikeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(String email) {
        User user = getUser(email);

        List<Portfolio> myPortfolios = portfolioRepository.findAllByUserId(user.getId());
        List<Portfolio> likedPortfolios = portfolioLikeRepository.findLikedPortfoliosByUserId(user.getId());
        List<Portfolio> commentedPortfolios = getCommentedPortfolios(user.getId());

        return MyPageResponse.of(
                toSummaries(myPortfolios, user.getId()),
                toSummaries(likedPortfolios, user.getId()),
                toSummaries(commentedPortfolios, user.getId())
        );
    }

    private List<Portfolio> getCommentedPortfolios(Long userId) {
        List<Long> orderedIds = commentRepository.findCommentedPortfolioIdsByUserId(userId);
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Portfolio> byId = portfolioRepository.findAllWithUserByIdIn(orderedIds).stream()
                .collect(Collectors.toMap(Portfolio::getId, Function.identity()));

        return orderedIds.stream()
                .map(byId::get)
                .filter(portfolio -> portfolio != null)
                .toList();
    }

    private List<PortfolioSummaryResponse> toSummaries(List<Portfolio> portfolios, Long userId) {
        if (portfolios.isEmpty()) {
            return List.of();
        }

        List<Long> portfolioIds = portfolios.stream()
                .map(Portfolio::getId)
                .toList();

        Map<Long, Long> likeCounts = getLikeCounts(portfolioIds);
        Set<Long> likedPortfolioIds = getLikedPortfolioIds(userId, portfolioIds);

        return portfolios.stream()
                .map(portfolio -> PortfolioSummaryResponse.from(
                        portfolio,
                        likeCounts.getOrDefault(portfolio.getId(), 0L),
                        likedPortfolioIds.contains(portfolio.getId())
                ))
                .toList();
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

    private Set<Long> getLikedPortfolioIds(Long userId, Collection<Long> portfolioIds) {
        if (portfolioIds.isEmpty()) {
            return Collections.emptySet();
        }

        return portfolioLikeRepository.findLikedPortfolioIdsByUserIdAndPortfolioIds(userId, portfolioIds).stream()
                .collect(Collectors.toSet());
    }

    private User getUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("인증 정보가 필요합니다.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
