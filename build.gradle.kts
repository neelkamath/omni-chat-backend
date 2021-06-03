plugins {
    application
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

version = "0.20.0"
application.mainClass.set("io.ktor.server.netty.EngineMain")

repositories { mavenCentral() }

dependencies {
    implementation("com.graphql-java:graphql-java:16.2")
    implementation("org.redisson:redisson:3.15.0")
    implementation("org.postgresql:postgresql:42.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.1")
    implementation("org.jasypt:jasypt:1.9.3")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit5"))

    val ktorVersion = "1.5.4"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    val exposedVersion = "0.31.1"
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
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
    }
    register("printVersion") { println(project.version) }
    val jvmTarget = "11"
    compileKotlin { kotlinOptions.jvmTarget = jvmTarget }
    compileTestKotlin { kotlinOptions.jvmTarget = jvmTarget }
}
