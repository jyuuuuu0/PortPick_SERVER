FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 1001 appuser
USER appuser

# 사전에 `./gradlew bootJar -x test` 로 빌드된 boot jar을 복사
# (-plain.jar 은 제외하기 위해 글롭을 -SNAPSHOT.jar 로 한정)
COPY build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
