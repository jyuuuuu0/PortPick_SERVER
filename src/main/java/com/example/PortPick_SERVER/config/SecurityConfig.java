package com.example.PortPick_SERVER.config;

import com.example.PortPick_SERVER.filter.JwtAuthenticationFilter;
import com.example.PortPick_SERVER.handler.OAuth2FailureHandler;
import com.example.PortPick_SERVER.handler.OAuth2SuccessHandler;
import com.example.PortPick_SERVER.jwt.JwtProvider;
import com.example.PortPick_SERVER.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtProvider jwtProvider;
    private final String authCookieName;

    public SecurityConfig(
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler,
            JwtProvider jwtProvider,
            @Value("${app.auth.cookie-name:PORTPICK_ACCESS_TOKEN}") String authCookieName
    ) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.jwtProvider = jwtProvider;
        this.authCookieName = authCookieName;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/error").permitAll()
                        .requestMatchers("/images/**", "/uploads/profiles/**", "/uploads/portfolios/**").permitAll()
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        .requestMatchers("/api/v1/auth/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/v1/auth/login/failure").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/portfolios", "/api/v1/portfolios/*", "/api/v1/portfolios/*/likes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/portfolios/*/comments").permitAll()
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/profile/**").authenticated()
                        .requestMatchers("/api/v1/mypage/**").authenticated()
                        .requestMatchers("/api/v1/comments/**").authenticated()
                        .requestMatchers("/api/v1/replies/**").authenticated()
                        .requestMatchers("/api/v1/portfolios/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(redirection -> redirection.baseUri("/api/v1/auth/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("""
                                    {"authenticated":false,"message":"Authentication is required."}
                                    """.trim());
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, authCookieName), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
