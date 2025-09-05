// Felles kode for alle build.gradle.kts filer som laster api-intern.conventions pluginen

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "no.nav.aap.api.intern"
version = project.findProperty("version")?.toString() ?: "0.0.0"

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")