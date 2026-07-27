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

# Render's containers don't have outbound IPv6 routing. Without this, the JVM's default
# dual-stack DNS resolution can still pick an IPv6 address for hosts that publish both
# A and AAAA records, causing "Network is unreachable" even against IPv4-capable hosts
# (e.g. Supabase's pooler). Forcing IPv4-only avoids that entirely.
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 8080
ENTRYPOINT ["/app/bin/btween-server"]
