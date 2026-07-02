package com.example.PortPick_SERVER.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
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
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH_SCHEME))
                .path("/oauth2/authorization/google", oauth2LoginStartPath())
                .path("/api/v1/auth/login/oauth2/code/google", oauth2CallbackPath());
    }

    /**
     * OAuth2 로그인 시작 엔드포인트. 실제로는 Spring Security 필터가 처리하므로
     * 컨트롤러가 없어 자동 스캔되지 않는다. 문서화를 위해 수동으로 등록한다.
     */
    private PathItem oauth2LoginStartPath() {
        Operation operation = new Operation()
                .addTagsItem("auth-controller")
                .summary("소셜 로그인 시작 (Google)")
                .description("""
                        구글 OAuth2 로그인을 시작한다. 브라우저에서 이 URL로 직접 접속하면
                        구글 로그인 화면으로 리다이렉트된다.
                        Spring Security가 처리하는 엔드포인트이므로 Swagger의 'Try it out'으로는
                        테스트할 수 없다(팝업/리다이렉트 방식).""")
                .responses(new ApiResponses()
                        .addApiResponse("302", new ApiResponse()
                                .description("구글 로그인 페이지로 리다이렉트")))
                .security(List.of());

        return new PathItem().get(operation);
    }

    /**
     * OAuth2 로그인 콜백 엔드포인트. 구글 인증 후 리다이렉트되며,
     * 성공 시 JWT 액세스 토큰 쿠키를 발급하고 앱으로 리다이렉트한다.
     */
    private PathItem oauth2CallbackPath() {
        Operation operation = new Operation()
                .addTagsItem("auth-controller")
                .summary("소셜 로그인 콜백 (Google)")
                .description("""
                        구글 인증 완료 후 리다이렉트되는 콜백 엔드포인트.
                        성공 시 JWT 액세스 토큰을 HttpOnly 쿠키로 발급한 뒤 앱으로 리다이렉트한다.
                        Spring Security가 자동으로 처리하며 클라이언트가 직접 호출하지 않는다.""")
                .responses(new ApiResponses()
                        .addApiResponse("302", new ApiResponse()
                                .description("인증 성공 시 JWT 쿠키 발급 후 앱으로 리다이렉트, 실패 시 실패 URL로 리다이렉트")))
                .security(List.of());

        return new PathItem().get(operation);
    }
}
