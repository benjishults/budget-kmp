plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
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
    implementation(projects.konfiguration)
    implementation(libs.jackson.jdk8)
    implementation(libs.konf)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty)

    testImplementation(libs.ktor.server.test.host)
//    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk.jvm)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
