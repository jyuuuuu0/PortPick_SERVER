package com.example.PortPick_SERVER.controller;

import com.example.PortPick_SERVER.dto.PortfolioSummaryResponse;
import com.example.PortPick_SERVER.service.MyPageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    @GetMapping("/portfolios")
    public ResponseEntity<List<PortfolioSummaryResponse>> getMyPortfolios(Authentication authentication) {
        return ResponseEntity.ok(myPageService.getMyPortfolios(authentication.getName()));
    }

    @GetMapping("/liked-portfolios")
    public ResponseEntity<List<PortfolioSummaryResponse>> getLikedPortfolios(Authentication authentication) {
        return ResponseEntity.ok(myPageService.getLikedPortfolios(authentication.getName()));
    }

    @GetMapping("/commented-portfolios")
    public ResponseEntity<List<PortfolioSummaryResponse>> getCommentedPortfolios(Authentication authentication) {
        return ResponseEntity.ok(myPageService.getCommentedPortfolios(authentication.getName()));
    }
}
