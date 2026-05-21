package com.example.PortPick_SERVER.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationTime;

    // application.properties에 적은 설정값들을 자동으로 읽어옵니다.
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    // ⚡ 유저 이메일을 기반으로 일하는 만료시간 30분짜리 Access Token 발급기
    public String createAccessToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(email) // 토큰 주인의 이메일 저장
                .issuedAt(now)   // 토큰 발행 시간
                .expiration(expiryDate) // 토큰 만료 시간
                .signWith(secretKey)    // 비밀키로 암호화 사인
                .compact();
    }
}