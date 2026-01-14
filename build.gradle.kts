val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

group = "com.yodgorbek.trendoraai"
version = "0.0.1"

application {
    mainClass.set("com.yodgorbek.trendoraai.backend.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-swagger:2.3.12")

    // Ktor client - USING CIO INSTEAD OF APACHE
    implementation("io.ktor:ktor-client-core-jvm:2.3.12")
    // implementation("io.ktor:ktor-client-apache-jvm:2.3.12") // REMOVED
    implementation("io.ktor:ktor-client-cio-jvm:2.3.12") // ADDED
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.12")

    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
