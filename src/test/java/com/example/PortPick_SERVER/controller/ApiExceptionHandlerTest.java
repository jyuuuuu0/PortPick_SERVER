package com.example.PortPick_SERVER.controller;

import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void 업로드_용량_초과시_413을_반환한다() {
        ResponseEntity<?> response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody().toString()).contains("업로드 가능한 파일 용량을 초과했습니다.");
    }

    @Test
    void Tomcat이_감싼_파일_용량_초과_예외도_413을_반환한다() {
        IllegalStateException wrapped = new IllegalStateException(
                new FileSizeLimitExceededException("too large", 2_000_000, 1_048_576));

        ResponseEntity<?> response = handler.handleIllegalState(wrapped);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    }

    @Test
    void 일반_IllegalStateException은_기존대로_401을_반환한다() {
        ResponseEntity<?> response = handler.handleIllegalState(new IllegalStateException("인증 실패"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
