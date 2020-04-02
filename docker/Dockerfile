# Gradle use carriage returns when logging by default, which aren't visible during Docker builds. Since gradle
# invocations take several minutes, it looks like it froze. Hence, we set the log level to info to see the progress.
FROM gradle:6.3-jdk8 AS builder
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
COPY --from=builder /app/build/libs/omni-chat-all.jar .
COPY --from=builder /app/build/resources/main/ src/main/resources/
COPY docker/dockerize docker/dockerize
HEALTHCHECK --timeout=5s --start-period=5s --retries=1 \
    CMD wget -q http://localhost:80/health_check || exit 1