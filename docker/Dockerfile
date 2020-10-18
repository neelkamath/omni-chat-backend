FROM gradle:6.6.0-jdk14 AS builder
WORKDIR /home/gradle/project
COPY . .
RUN gradle -i shadowJar

FROM adoptopenjdk:14-jre-hotspot
WORKDIR /application
COPY --from=builder /home/gradle/project/build/libs/*.jar omni-chat.jar
EXPOSE 80
HEALTHCHECK --timeout=5s --start-period=5s --retries=1 \
    CMD curl -f http://localhost:80/health-check
