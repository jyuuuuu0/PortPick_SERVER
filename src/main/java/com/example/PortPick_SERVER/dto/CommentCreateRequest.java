package com.example.PortPick_SERVER.dto;

public class CommentCreateRequest {

    private String content;
    private Double xPercent;
    private Double yPercent;

    public String getContent() {
        return content;
    }

    public Double getXPercent() {
        return xPercent;
    }

    public Double getYPercent() {
        return yPercent;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setXPercent(Double xPercent) {
        this.xPercent = xPercent;
    }

    public void setYPercent(Double yPercent) {
        this.yPercent = yPercent;
    }
}
