# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and build files first so dependency resolution can be cached
# as its own Docker layer, separate from source code changes.
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew installDist --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/btween-server /app

EXPOSE 8080
ENTRYPOINT ["/app/bin/btween-server"]
