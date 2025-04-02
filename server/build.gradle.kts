plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
    alias(libs.plugins.serialization)
    id("com.gradleup.shadow") version "8.3.6"
}

group = "bps.budget"
version = "1.0.0"
application {
    mainClass.set("bps.budget.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    // TODO seems a waste to pull in all this UI code when all I really need is to share the server port.
    implementation(projects.shared)
    implementation(projects.budgetDao)
    implementation(projects.allShared)
    implementation(projects.konfiguration)
    implementation(projects.jvmShared)
    implementation(libs.jackson.jdk8)
    implementation(libs.konf)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.logback)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.content.negotiation)
//    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk.jvm)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(projects.budgetDaoTest)
}

tasks.test {
    useJUnitPlatform()
}
