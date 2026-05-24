package com.example.PortPick_SERVER.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    private final String profileUploadUrlPrefix;
    private final Path profileUploadDir;

    public WebResourceConfig(
            @Value("${app.profile.upload-url-prefix:/uploads/profiles}") String profileUploadUrlPrefix,
            @Value("${app.profile.upload-dir:uploads/profiles}") String profileUploadDir
    ) {
        this.profileUploadUrlPrefix = profileUploadUrlPrefix;
        this.profileUploadDir = Path.of(profileUploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(profileUploadUrlPrefix + "/**")
                .addResourceLocations(profileUploadDir.toUri().toString());
    }
}
