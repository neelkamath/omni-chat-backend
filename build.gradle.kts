import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    kotlin("jvm") version "1.3.71"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.breadmoirai.github-release") version "2.2.10"
}

version = "0.0.1"
application.mainClassName = "io.ktor.server.netty.EngineMain"

repositories { jcenter() }

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    val ktorVersion = "1.3.2"
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.1")
}

tasks {
    withType<Jar> {
        manifest { attributes(mapOf("Main-Class" to application.mainClassName)) }
    }
    withType<ShadowJar> { archiveVersion.set("") }
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