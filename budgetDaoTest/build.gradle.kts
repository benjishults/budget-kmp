plugins {
    alias(libs.plugins.kotlinJvm)
    kotlin("plugin.allopen") version "2.1.10"
    `java-library`
}

group = "bps"
version = "1.0-SNAPSHOT"

allOpen {
    annotations("bps.kotlin.Instrumentable")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
//        freeCompilerArgs.add("-Xcontext-receivers")
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

dependencies {

    // TODO see how many of these I can get rid of
    implementation(projects.allShared)
    implementation(projects.budgetDao)
    implementation(libs.commons.validator)
    runtimeOnly(libs.postgres)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jackson.jsr310)
    implementation(libs.jackson.jdk8)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation(libs.mockk.jvm)
    implementation(libs.kotest.junit5)
    implementation(libs.kotest.assertions)
    implementation(libs.junit.jupiter)
}
