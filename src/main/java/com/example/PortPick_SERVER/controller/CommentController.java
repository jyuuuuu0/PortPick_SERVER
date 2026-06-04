package com.example.PortPick_SERVER.controller;

import com.example.PortPick_SERVER.dto.CommentCreateRequest;
import com.example.PortPick_SERVER.dto.CommentResponse;
import com.example.PortPick_SERVER.dto.ReplyCreateRequest;
import com.example.PortPick_SERVER.dto.ReplyResponse;
import com.example.PortPick_SERVER.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/v1/portfolios/{portfolioId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            Authentication authentication,
            @PathVariable Long portfolioId
    ) {
        return ResponseEntity.ok(
                commentService.getComments(
                        authentication != null ? authentication.getName() : null,
                        portfolioId
                )
        );
    }

    @PostMapping("/api/v1/portfolios/{portfolioId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            Authentication authentication,
            @PathVariable Long portfolioId,
            @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(authentication.getName(), portfolioId, request));
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            Authentication authentication,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(authentication.getName(), commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/comments/{commentId}/replies")
    public ResponseEntity<ReplyResponse> createReply(
            Authentication authentication,
            @PathVariable Long commentId,
            @RequestBody ReplyCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createReply(authentication.getName(), commentId, request));
    }

    @DeleteMapping("/api/v1/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            Authentication authentication,
            @PathVariable Long replyId
    ) {
        commentService.deleteReply(authentication.getName(), replyId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/comments/{commentId}/resolve")
    public ResponseEntity<CommentResponse> resolveComment(
            Authentication authentication,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(commentService.resolveComment(authentication.getName(), commentId));
    }
}
