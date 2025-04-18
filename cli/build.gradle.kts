import java.nio.file.Paths

plugins {
    alias(libs.plugins.kotlinJvm)
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "bps"
version = "1.0-SNAPSHOT"

application {
    mainClass = "bps.budget.Budget"
}

val classPathDirectory =
    file(
        System
            .getenv("BPS_BUDGET_CLI_CLASSPATH")
            ?: Paths.get(
                System.getProperty("user.home"),
                ".local",
                "share",
                "bps-budget",
                "libs",
            ),
    )

tasks.register<Copy>("copyShadowJarToClasspath") {
    dependsOn("shadowJar")
    from(layout.buildDirectory.file("libs/cli-1.0-SNAPSHOT-all.jar"))
    into(classPathDirectory)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/benjishults/console")
        credentials {
            username = providers
                .gradleProperty("github.actor")
                .getOrElse(System.getenv("GITHUB_ACTOR"))
            password = providers
                .gradleProperty("github.token")
                .getOrElse(System.getenv("GITHUB_TOKEN"))
        }
    }
}

//kotlin {
//    jvmToolchain(21)
//}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
//        freeCompilerArgs.add("-Xcontext-receivers")
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {

//    implementation("io.github.nhubbard:konf:$konfVersion")
//    implementation("io.github.config4k:config4k:0.7.0")
//    implementation("io.github.nhubbard:konf-core:$konfVersion")
//    implementation("io.github.nhubbard:konf-yaml:$konfVersion")
    // password hashing https://javadoc.io/doc/de.mkammerer/argon2-jvm/2.6/de/mkammerer/argon2/Argon2.html
    //   https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#argon2id
//    implementation("de.mkammerer:argon2-jvm:2.11")
    implementation(projects.budgetDao)
    implementation(projects.konfiguration) {
        exclude(group = "slf4j.org", module = "slf4j-api")
    }
    implementation(projects.allShared)
    implementation(libs.commons.validator)
    implementation(libs.bps.console)
    runtimeOnly(libs.postgres)
    implementation(libs.konf)
//    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation(libs.kotlinx.datetime)
    implementation(libs.jackson.jsr310)
    implementation(libs.jackson.jdk8)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(libs.logback)

//    testImplementation(libs.junit.bom)
    testImplementation(projects.budgetDaoTest)
    testImplementation(libs.bps.console.test)
    testImplementation(libs.mockk.jvm)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
