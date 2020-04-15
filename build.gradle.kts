import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    application
    kotlin("jvm") version "1.3.71"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

version = "0.0.1"
application.mainClassName = "io.ktor.server.netty.EngineMain"

repositories { jcenter() }

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    val ktorVersion = "1.3.2"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.1")
    val exposedVersion = "0.23.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.postgresql:postgresql:42.2.2")
    val keycloakVersion = "9.0.2"
    implementation("org.keycloak:keycloak-admin-client:$keycloakVersion")
    implementation("org.keycloak:keycloak-authz-client:$keycloakVersion")
    val kotestVersion = "4.0.2"
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
}

tasks {
    withType<Test> { useJUnitPlatform() }
    withType<Jar> {
        manifest { attributes(mapOf("Main-Class" to application.mainClassName)) }
    }
    withType<ShadowJar> { archiveVersion.set("") }
    named<KotlinJvmCompile>("compileTestKotlin") { kotlinOptions.jvmTarget = "1.8" }
}

if (gradle.startParameter.taskNames.contains("githubRelease"))
    githubRelease {
        token(property("GITHUB_PAT") as String)
        owner("neelkamath")
        body(File("docs/release.md").readText())
        overwrite(true)
        prerelease((project.version as String).startsWith("0"))
        releaseAssets("redoc-static.html")
    }