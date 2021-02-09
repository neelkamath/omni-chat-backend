FROM gradle:6.8.2-jdk15-hotspot
RUN curl -s \
        https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/7.0.4/flyway-commandline-7.0.4-linux-x64.tar.gz \
    | tar xvz \
    && ln -s `pwd`/flyway-7.0.4/flyway /usr/local/bin
