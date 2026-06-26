package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.User;

public record PortfolioAuthorResponse(
        String name,
        String career,
        String jobRole
) {
    public static PortfolioAuthorResponse from(User author) {
        return new PortfolioAuthorResponse(
                author.getName(),
                buildCareer(author),
                author.getJobRole() != null ? author.getJobRole().getLabel() : null
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
}
