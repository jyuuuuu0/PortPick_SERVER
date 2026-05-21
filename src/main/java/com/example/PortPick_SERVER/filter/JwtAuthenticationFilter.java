package com.example.PortPick_SERVER.filter;

import com.example.PortPick_SERVER.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // 1. 유저가 보낸 요청 헤더에서 "Authorization" 값을 꺼냅니다.
        String bearerToken = request.getHeader("Authorization");
        String token = null;

        // 2. 보통 토큰은 "Bearer ey..." 형태로 오기 때문에 앞에 붙은 "Bearer "를 떼어내고 진짜 토큰만 추출합니다.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }

        // 3. 토큰이 존재하고, 아까 만든 검증기가 "이거 진짜야!"라고 인증해 주면
        if (token != null && jwtProvider.validateToken(token)) {
            String email = jwtProvider.getEmailFromToken(token);

            // 4. 스프링 시큐리티 전용 "임시 신분증"을 발급합니다.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
            );

            // 5. 이 신분증을 시큐리티 보관함(Context)에 쏙 넣어두면, 이번 요청은 무사통과됩니다!
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터(문지기)에게 요청을 넘깁니다.
        filterChain.doFilter(request, response);
    }
}