import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

version = "0.6.1"
application.mainClassName = "io.ktor.server.netty.EngineMain"

repositories { jcenter() }

dependencies {
    implementation("com.graphql-java:graphql-java:15.0")
    implementation("org.redisson:redisson:3.13.3")
    implementation("org.postgresql:postgresql:42.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.0")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    val ktorVersion = "1.4.0"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    val exposedVersion = "0.26.2"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    val keycloakVersion = "11.0.0"
    implementation("org.keycloak:keycloak-admin-client:$keycloakVersion")
    implementation("org.keycloak:keycloak-authz-client:$keycloakVersion")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
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