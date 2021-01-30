plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

version = "0.12.0"
application.mainClassName = "io.ktor.server.netty.EngineMain"

repositories { jcenter() }

dependencies {
    implementation("com.graphql-java:graphql-java:16.1")
    implementation("org.redisson:redisson:3.14.1")
    implementation("org.postgresql:postgresql:42.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.0")
    implementation("org.jasypt:jasypt:1.9.3")
    implementation("com.sun.mail:javax.mail:1.6.2")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    val ktorVersion = "1.5.1"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    val exposedVersion = "0.28.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
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
    register("printVersion") { println(project.version) }
    val jvmTarget = "14"
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
