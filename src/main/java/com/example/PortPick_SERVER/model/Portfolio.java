package com.example.PortPick_SERVER.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(length = 1000)
    private String embedLink;

    @Column(length = 1000)
    private String fileUrl;

    @Column(length = 255)
    private String originalFileName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Portfolio(User user, String title, String description, String embedLink, String fileUrl, String originalFileName) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.embedLink = embedLink;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
    }

    public void updateText(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
