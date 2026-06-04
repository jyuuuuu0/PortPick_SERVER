package com.example.PortPick_SERVER.controller;

import com.example.PortPick_SERVER.dto.PortfolioCreateRequest;
import com.example.PortPick_SERVER.dto.PortfolioDetailResponse;
import com.example.PortPick_SERVER.dto.PortfolioUpdateRequest;
import com.example.PortPick_SERVER.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PortfolioDetailResponse> createPortfolio(
            Authentication authentication,
            @ModelAttribute PortfolioCreateRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.createPortfolio(authentication.getName(), request, file));
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioDetailResponse> updatePortfolio(
            Authentication authentication,
            @PathVariable Long portfolioId,
            @ModelAttribute PortfolioUpdateRequest request
    ) {
        return ResponseEntity.ok(portfolioService.updatePortfolio(authentication.getName(), portfolioId, request));
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(
            Authentication authentication,
            @PathVariable Long portfolioId
    ) {
        portfolioService.deletePortfolio(authentication.getName(), portfolioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioDetailResponse> getPortfolioDetail(@PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolioDetail(portfolioId));
    }
}
