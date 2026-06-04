package com.example.PortPick_SERVER.dto;

import java.util.List;

public record MyPageResponse(
        List<PortfolioSummaryResponse> myPortfolios,
        List<PortfolioSummaryResponse> likedPortfolios,
        List<PortfolioSummaryResponse> commentedPortfolios
) {
    public static MyPageResponse of(
            List<PortfolioSummaryResponse> myPortfolios,
            List<PortfolioSummaryResponse> likedPortfolios,
            List<PortfolioSummaryResponse> commentedPortfolios
    ) {
        return new MyPageResponse(myPortfolios, likedPortfolios, commentedPortfolios);
    }
}
