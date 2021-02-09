FROM gradle:6.8.2-jdk15-hotspot AS builder
WORKDIR /home/gradle/project
COPY build.gradle.kts gradle.properties settings.gradle.kts ./
RUN gradle shadowJar
COPY src/main/ src/main/
RUN gradle shadowJar

FROM adoptopenjdk:15-jre-hotspot
WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/*.jar omni-chat.jar
COPY src/main/resources/db/migration/ .
RUN curl -s \
        https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/7.0.4/flyway-commandline-7.0.4-linux-x64.tar.gz \
    | tar xvz \
    && ln -s `pwd`/flyway-7.0.4/flyway /usr/local/bin
EXPOSE 80
HEALTHCHECK --start-period=5s --retries=1 --timeout=3s \
    CMD curl -f http://localhost/health-check
ENV FLYWAY_LOCATIONS=filesystem:/app \
    FLYWAY_URL=jdbc:postgresql://$POSTGRES_URL/$POSTGRES_DB \
    FLYWAY_USER=$POSTGRES_USER \
    FLYWAY_PASSWORD=$POSTGRES_PASSWORD
CMD sh -c 'flyway migrate && java -server -jar omni-chat.jar'
