# syntax=docker/dockerfile:1.7

# ============================================
# ETAPA 1: COMPILACIÓN
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace/app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew clean bootJar -x test --no-daemon \
    && JAR_FILE="$(find build/libs -maxdepth 1 -type f \
        -name '*.jar' ! -name '*-plain.jar' | head -n 1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /workspace/app.jar


# ============================================
# ETAPA 2: EJECUCIÓN
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache curl tzdata \
    && addgroup -S spring \
    && adduser -S spring -G spring

COPY --from=builder \
    --chown=spring:spring \
    /workspace/app.jar \
    /app/app.jar

COPY --chown=spring:spring \
    docker-entrypoint.sh \
    /app/docker-entrypoint.sh

RUN sed -i 's/\r$//' /app/docker-entrypoint.sh \
    && chmod +x /app/docker-entrypoint.sh

USER spring:spring

EXPOSE 8080

ENV TZ=America/Guayaquil

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=60s \
    --retries=3 \
    CMD curl --fail --silent --show-error \
    "http://localhost:${PORT:-8080}/api/actuator/health" \
    || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]