plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

// NOTE This was part of the (now-abandoned and unneeded) effort to get the server to run in a container
//val dockerFileDir = layout.projectDirectory.file("ci/server/")
//
//tasks.register<Copy>("copyServerShadowJarToDockerFolder") {
//    dependsOn("server:shadowJar")
//    from(layout.projectDirectory.file("server/build/libs/server-all.jar"))
//    into(dockerFileDir)
//}
