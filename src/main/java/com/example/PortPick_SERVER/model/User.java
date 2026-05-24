package com.example.PortPick_SERVER.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users") // MySQL에 users라는 이름의 테이블로 생성됨
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // 구글에서 받아올 이메일 (로그인 ID 역할)

    @Column(nullable = false)
    private String name; // 구글 프로필 이름

    @Column
    private String provider; // 소셜 로그인 종류 (여기서는 "google" 저장)

    @Builder
    public User(String email, String name, String provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    // 이름이 바뀌었을 때를 위한 업데이트 메서드
    public User update(String name) {
        this.name = name;
        return this;
    }
}