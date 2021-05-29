FROM gradle:7.0.2-jdk11-hotspot
RUN apt-get update && apt-get install -y fish
RUN curl -s \
        https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/7.0.4/flyway-commandline-7.0.4-linux-x64.tar.gz \
    | tar xvz \
    && ln -s `pwd`/flyway-7.0.4/flyway /usr/local/bin
