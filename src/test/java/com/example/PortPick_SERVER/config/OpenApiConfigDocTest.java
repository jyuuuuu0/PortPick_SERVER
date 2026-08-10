package com.example.PortPick_SERVER.config;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigDocTest {

    @Test
    void oauth2LoginEndpointsAreDocumented() throws Exception {
        OpenAPI api = new OpenApiConfig("PORTPICK_ACCESS_TOKEN").portPickOpenAPI();

        assertThat(api.getPaths()).containsKeys(
                "/oauth2/authorization/google",
                "/api/v1/auth/login/oauth2/code/google");

        for (String p : new String[]{
                "/oauth2/authorization/google",
                "/api/v1/auth/login/oauth2/code/google"}) {
            PathItem item = api.getPaths().get(p);
            System.out.println("PATH " + p
                    + " | summary=" + item.getGet().getSummary()
                    + " | tags=" + item.getGet().getTags()
                    + " | security=" + item.getGet().getSecurity());
        }

        System.out.println("=== auth-related paths in generated spec ===");
        api.getPaths().keySet().stream()
                .filter(k -> k.contains("auth") || k.contains("oauth2"))
                .forEach(System.out::println);
    }
}
