package com.example.PortPick_SERVER.handler;

import com.example.PortPick_SERVER.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final String authorizedRedirectUrl;
    private final String authCookieName;
    private final boolean cookieSecure;

    public OAuth2SuccessHandler(
            JwtProvider jwtProvider,
            @Value("${app.oauth2.authorized-redirect-url:http://localhost:8081/}") String authorizedRedirectUrl,
            @Value("${app.auth.cookie-name:PORTPICK_ACCESS_TOKEN}") String authCookieName,
            @Value("${app.auth.cookie-secure:false}") boolean cookieSecure) {
        this.jwtProvider = jwtProvider;
        this.authorizedRedirectUrl = authorizedRedirectUrl;
        this.authCookieName = authCookieName;
        this.cookieSecure = cookieSecure;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "missing_email",
                    "Google account email is missing from the OAuth2 response.",
                    null
            ));
        }

        String accessToken = jwtProvider.createAccessToken(email);

        ResponseCookie authCookie = ResponseCookie.from(authCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtProvider.getExpirationTime()))
                .build();

        response.addHeader("Set-Cookie", authCookie.toString());
        getRedirectStrategy().sendRedirect(request, response, authorizedRedirectUrl);
    }
}
