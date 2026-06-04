package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.Portfolio;
import com.example.PortPick_SERVER.model.User;

import java.time.LocalDateTime;

public record PortfolioDetailResponse(
        Long id,
        String title,
        String description,
        String embedLink,
        String fileUrl,
        String originalFileName,
        LocalDateTime createdAt,
        AuthorSummary author
) {
    public static PortfolioDetailResponse from(Portfolio portfolio) {
        User author = portfolio.getUser();

        return new PortfolioDetailResponse(
                portfolio.getId(),
                portfolio.getTitle(),
                portfolio.getDescription(),
                portfolio.getEmbedLink(),
                portfolio.getFileUrl(),
                portfolio.getOriginalFileName(),
                portfolio.getCreatedAt(),
                new AuthorSummary(
                        author.getName(),
                        buildCareer(author),
                        author.getJobRole() != null ? author.getJobRole().getLabel() : null
                )
        );
    }

    private static String buildCareer(User author) {
        if (author.getCareerType() == null) {
            return null;
        }
        if (author.getCareerRange() == null) {
            return author.getCareerType().getLabel();
        }
        return author.getCareerType().getLabel() + " " + author.getCareerRange().getLabel();
    }

    public record AuthorSummary(
            String name,
            String career,
            String jobRole
    ) {
    }
}
