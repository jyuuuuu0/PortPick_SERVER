package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.Portfolio;

import java.time.LocalDateTime;

public record PortfolioSummaryResponse(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt,
        PortfolioAuthorResponse author,
        long likeCount,
        boolean liked
) {
    public static PortfolioSummaryResponse from(Portfolio portfolio, long likeCount, boolean liked) {
        return new PortfolioSummaryResponse(
                portfolio.getId(),
                portfolio.getTitle(),
                portfolio.getDescription(),
                portfolio.getCreatedAt(),
                PortfolioAuthorResponse.from(portfolio.getUser()),
                likeCount,
                liked
        );
    }
}
