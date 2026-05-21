package com.example.PortPick_SERVER.handler;

import com.example.PortPick_SERVER.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1. 구글이 준 로그인 유저 정보를 시큐리티 세션에서 꺼냅니다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        // 2. 아까 만든 JwtProvider를 사용해서 이메일 기반의 Access Token을 생성합니다!
        String accessToken = jwtProvider.createAccessToken(email);

        // 3. 프론트엔드(리액트 등)가 기다리고 있는 주소로 토큰을 쿼리 스트링 파라미터에 담아 보냅니다.
        // (지금은 프론트엔드가 없으니 일단 테스트용 주소나 메인 루트 주소로 보낼게요!)
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8081/")
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        // 4. 해당 주소로 강제 이동(리다이렉트) 시킵니다.
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}