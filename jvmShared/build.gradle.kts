import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-library`
    alias(libs.plugins.serialization)
}

group = "bps"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.named("compileKotlin", KotlinCompilationTask::class.java) {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
    }
}

dependencies {

    // TODO see how many of these I can get rid of
    implementation(projects.allShared)
//    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json.jvm)

    testImplementation(libs.mockk.jvm)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
