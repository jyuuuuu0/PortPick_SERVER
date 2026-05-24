package com.example.PortPick_SERVER.controller;

import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final String authCookieName;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${app.auth.cookie-name:PORTPICK_ACCESS_TOKEN}") String authCookieName,
            @Value("${app.auth.cookie-secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.authCookieName = authCookieName;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthStatusResponse> me(Authentication authentication) {
        User user = authService.getAuthenticatedUser(authentication.getName());

        return ResponseEntity.ok(new AuthStatusResponse(
                true,
                user.getEmail(),
                user.getName(),
                user.getProvider()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(authCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private record AuthStatusResponse(
            boolean authenticated,
            String email,
            String name,
            String provider
    ) {
    }
}
