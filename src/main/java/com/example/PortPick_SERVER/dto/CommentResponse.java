package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String content,
        double xPercent,
        double yPercent,
        boolean resolved,
        CommentAuthorResponse author,
        List<ReplyResponse> replies,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getXPercent(),
                comment.getYPercent(),
                comment.isResolved(),
                CommentAuthorResponse.from(comment.getUser()),
                comment.getReplies().stream()
                        .map(ReplyResponse::from)
                        .toList(),
                comment.getCreatedAt()
        );
    }
}
