package com.example.PortPick_SERVER.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void 허용_출처를_쉼표로_나눠_등록하고_쿠키를_허용한다() {
        CorsConfig corsConfig = new CorsConfig("http://localhost:3000, https://portpick.app");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/portfolios");

        CorsConfiguration config = corsConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:3000", "https://portpick.app");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.checkOrigin("https://evil.example")).isNull();
    }
}
