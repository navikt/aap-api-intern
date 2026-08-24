import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("no.nav.aap.api.AppKt")
}

dependencies {
    implementation(project(":kontrakt"))

    implementation(libs.kelvin.server)
    implementation(libs.kelvin.infrastructure)
    implementation(libs.kelvin.dbconnect)
    implementation(libs.kelvin.dbmigrering)
    implementation(libs.kelvin.tidslinje)
    implementation(libs.tilgang.plugin)
    implementation(libs.behandlingsflyt.kontrakt)
    implementation(libs.oppgave.api.kontrakt)
    implementation(libs.arenaoppslag.kontrakt)
    implementation(libs.kelvin.motor)
    implementation(libs.kelvin.motor.api)

    implementation(libs.ktor.serialization.jackson)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.logging.jvm)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.id)

    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.jackson)
    implementation(libs.ktor.client.logging)

    implementation(libs.kafka.clients)
    implementation(libs.logback)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.prometheus.metrics.tracer.initializer)

    implementation(libs.hikaricp)

    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.kotlin)
    implementation(libs.resilience4j.micrometer)

    implementation(libs.caffeine)

    implementation(libs.kelvin.ktor.openapi.generator)
    testImplementation(libs.kelvin.dbtest)
    testImplementation(libs.ktor.server.test.host)
    constraints {
        implementation(libs.commons.codec)
    }
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj)
    testImplementation(libs.nimbus.jose.jwt)
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}

tasks.register<JavaExec>("runTestApp") {
    group = "application"
    description = "Kjør TestApp"
    mainClass.set("no.nav.aap.api.TestAppKt")
    classpath = sourceSets["test"].runtimeClasspath
}
