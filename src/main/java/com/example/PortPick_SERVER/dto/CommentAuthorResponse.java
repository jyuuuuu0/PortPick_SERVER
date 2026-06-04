package com.example.PortPick_SERVER.dto;

import com.example.PortPick_SERVER.model.User;

public record CommentAuthorResponse(
        Long id,
        String name,
        String profileImageUrl
) {
    public static CommentAuthorResponse from(User user) {
        return new CommentAuthorResponse(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}
