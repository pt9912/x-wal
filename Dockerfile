# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :app:build :app:buildLayers -x test --no-daemon

# Runtime stage — uses Micronaut layered jars for optimal Docker caching
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S xwal && adduser -S xwal -G xwal
WORKDIR /app

# Copy layers separately for Docker cache optimization
# (libs change rarely, app changes often)
COPY --from=build /app/app/build/docker/main/layers/libs ./libs/
COPY --from=build /app/app/build/docker/main/layers/resources ./resources/
COPY --from=build /app/app/build/docker/main/layers/project_libs ./project_libs/
COPY --from=build /app/app/build/docker/main/layers/snapshot_libs ./snapshot_libs/
COPY --from=build /app/app/build/docker/main/layers/app ./

USER xwal

EXPOSE 8080 50051

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-cp", ".:./libs/*:./resources:./project_libs/*:./snapshot_libs/*", \
  "com.xwal.Application"]
