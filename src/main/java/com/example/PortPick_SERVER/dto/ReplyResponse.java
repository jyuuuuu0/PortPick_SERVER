package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.Reply;

import java.time.LocalDateTime;

public record ReplyResponse(
        Long id,
        String content,
        CommentAuthorResponse author,
        LocalDateTime createdAt
) {
    public static ReplyResponse from(Reply reply) {
        return new ReplyResponse(
                reply.getId(),
                reply.getContent(),
                CommentAuthorResponse.from(reply.getUser()),
                reply.getCreatedAt()
        );
    }
}
