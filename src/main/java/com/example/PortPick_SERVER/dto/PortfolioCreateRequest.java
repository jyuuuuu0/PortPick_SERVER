package com.example.PortPick_SERVER.dto;

public class PortfolioCreateRequest {

    private String title;
    private String description;
    private String embedLink;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getEmbedLink() {
        return embedLink;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEmbedLink(String embedLink) {
        this.embedLink = embedLink;
    }
}
