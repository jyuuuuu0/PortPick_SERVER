package com.example.PortPick_SERVER.dto;

public record PortfolioLikeResponse(
        Long portfolioId,
        long likeCount,
        boolean liked
) {
}
