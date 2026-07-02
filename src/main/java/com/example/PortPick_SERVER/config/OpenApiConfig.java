package com.example.PortPick_SERVER.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String COOKIE_AUTH_SCHEME = "cookieAuth";

    private final String authCookieName;

    public OpenApiConfig(@Value("${app.auth.cookie-name:PORTPICK_ACCESS_TOKEN}") String authCookieName) {
        this.authCookieName = authCookieName;
    }

    @Bean
    public OpenAPI portPickOpenAPI() {
        SecurityScheme cookieAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(authCookieName)
                .description("Google OAuth2 로그인 후 발급되는 JWT 액세스 토큰 쿠키");

        return new OpenAPI()
                .info(new Info()
                        .title("PortPick API")
                        .description("PortPick 포트폴리오 공유 플랫폼 REST API 문서")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(COOKIE_AUTH_SCHEME, cookieAuth))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH_SCHEME));
    }
}
