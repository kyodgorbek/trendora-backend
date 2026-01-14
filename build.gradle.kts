plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.ktor.plugin") version "2.3.12"
    application
}

application {
    mainClass.set("com.yodgorbek.trendoraai.backend.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-swagger:2.3.12")

    // Ktor client
    implementation("io.ktor:ktor-client-core-jvm:2.3.12")
    implementation("io.ktor:ktor-client-apache-jvm:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.12")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")
}
