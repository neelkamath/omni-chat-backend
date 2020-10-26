FROM gradle:6.6.0-jdk14
RUN curl -s \
        https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/7.0.4/flyway-commandline-7.0.4-linux-x64.tar.gz \
    | tar xvz \
    && ln -s `pwd`/flyway-7.0.4/flyway /usr/local/bin
