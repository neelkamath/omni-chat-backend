import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

version = "0.1.1"
application.mainClassName = "io.ktor.server.netty.EngineMain"

repositories { jcenter() }

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.graphql-java:graphql-java:15.0")
    implementation("org.postgresql:postgresql:42.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.0")
    testImplementation("io.mockk:mockk:1.10.0")

    /*
    ktor provides content negotiation via Jackson. Jackson is provided as a transitive dependency. Therefore, the
    version of Jackson modules we use must match the version of Jackson ktor uses. The version to be used can be found
    in https://github.com/ktorio/ktor/blob/3c54c686f46b591825bbdb6fe4ceea1659175290/gradle.properties.
     */
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")

    val ktorVersion = "1.3.2"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    val exposedVersion = "0.26.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    val keycloakVersion = "10.0.2"
    implementation("org.keycloak:keycloak-admin-client:$keycloakVersion")
    implementation("org.keycloak:keycloak-authz-client:$keycloakVersion")
    val kotestVersion = "4.1.1"
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        // Workaround for remote debugging a JVM 9+ target (https://github.com/gradle/gradle/issues/13118).
        jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
    }
    withType<Jar> {
        manifest { attributes(mapOf("Main-Class" to application.mainClassName)) }
    }
    withType<ShadowJar> {
        archiveVersion.set("")
        mergeServiceFiles()
    }
    register("printVersion") { println(project.version) }
    val jvmTarget = "13"
    compileKotlin { kotlinOptions.jvmTarget = jvmTarget }
    compileTestKotlin { kotlinOptions.jvmTarget = jvmTarget }
}

if (gradle.startParameter.taskNames.contains("githubRelease"))
    githubRelease {
        token(property("GITHUB_TOKEN") as String)
        owner("neelkamath")
        overwrite(true)
        prerelease((project.version as String).startsWith("0"))
        releaseAssets("rest-api.html")
    }