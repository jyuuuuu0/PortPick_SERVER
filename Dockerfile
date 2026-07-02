# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Gradle 래퍼 및 빌드 스크립트 먼저 복사 (의존성 레이어 캐싱)
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 소스 복사 후 실행 가능한 boot jar 빌드 (테스트는 빌드 파이프라인/CI에서 별도 수행)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 비루트 사용자로 실행
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=builder /workspace/build/libs/*.jar app.jar

# 앱 내부 포트 (server.port=8082) — 배포 시 호스트 80:8082 로 매핑
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
