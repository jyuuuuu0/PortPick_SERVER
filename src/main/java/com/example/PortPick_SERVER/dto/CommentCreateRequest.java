package com.example.PortPick_SERVER.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentCreateRequest {

    private String content;
    private Double xPercent;
    private Double yPercent;
}
