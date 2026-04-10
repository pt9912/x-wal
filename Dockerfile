# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :app:build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S xwal && adduser -S xwal -G xwal
WORKDIR /app
COPY --from=build /app/app/build/libs/app-runner.jar app.jar
USER xwal

EXPOSE 8080 50051

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-jar", "app.jar"]
