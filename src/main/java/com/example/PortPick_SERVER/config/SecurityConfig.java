package com.example.PortPick_SERVER.config;

import com.example.PortPick_SERVER.handler.OAuth2SuccessHandler;
import com.example.PortPick_SERVER.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler; // ⚡ 핸들러 주입!

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginProcessingUrl("/api/v1/auth/login/oauth2/code/*")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // ⚡ 로그인 완벽히 성공하면 이 핸들러를 실행해라! 추가
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }
}