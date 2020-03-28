# Gradle use carriage returns when logging by default, which aren't visible during Docker builds. Since gradle
# invocations take several minutes, it looks like it froze. Hence, we set the log level to info to see the progress.
FROM gradle:6.0.1-jdk8 AS builder
WORKDIR /app
# Without this environment variable, Gradle will not cache dependencies. When building this image without cached layers,
# it would seem that the environment variable is unnecessary, because the second Gradle task uses the cache from the
# first Gradle task. This is because they execute in the same context. When building this image with only the first
# Gradle task's layer cached, the second Gradle task will unnecessarily redownload dependencies.
ENV GRADLE_USER_HOME /cache
COPY build.gradle.kts gradle.properties settings.gradle.kts ./
# The shadowJar task maximizes cacheable outputs when compared to the build task, or the --refresh-dependencies flag.
RUN gradle --no-daemon -i shadowJar
COPY src/main/ src/main/
RUN gradle --no-daemon -i shadowJar

FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/crystal-skull-all.jar .
COPY --from=builder /app/build/resources/main/ src/main/resources/
COPY docker/ docker/
# VisualVM uses port 9010.
EXPOSE 80 9010
RUN apk --no-cache add bash curl
HEALTHCHECK --timeout=5s --start-period=5s --retries=1 \
    CMD curl -f http://localhost:80/health_check