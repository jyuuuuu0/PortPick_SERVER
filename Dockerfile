FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 1001 appuser
USER appuser

COPY build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]




