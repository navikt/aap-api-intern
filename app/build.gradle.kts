import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(kelvinLibs.plugins.ktor)
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

    implementation(kelvinLibs.ktor.serialization.jackson)

    implementation(kelvinLibs.ktor.server.auth)
    implementation(kelvinLibs.ktor.server.auth.jwt)
    implementation(kelvinLibs.ktor.server.call.logging)
    implementation(kelvinLibs.ktor.server.call.logging.jvm)
    implementation(kelvinLibs.ktor.server.content.negotiation)
    implementation(kelvinLibs.ktor.server.core)
    implementation(kelvinLibs.ktor.server.metrics.micrometer)
    implementation(kelvinLibs.ktor.server.status.pages)
    implementation(kelvinLibs.ktor.server.call.id)

        implementation(kelvinLibs.ktor.client.cio)
    implementation(kelvinLibs.ktor.client.content.negotiation)
    implementation(kelvinLibs.ktor.client.jackson)
    implementation(kelvinLibs.ktor.client.logging)

    implementation(kelvinLibs.kafka.clients)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.jackson.datatype.jsr310)
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logstash.logback.encoder)
    implementation(libs.prometheus.metrics.tracer.initializer)

    implementation(kelvinLibs.hikaricp)

    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.kotlin)
    implementation(libs.resilience4j.micrometer)

    implementation(kelvinLibs.caffeine)

    implementation(libs.kelvin.ktor.openapi.generator)
    testImplementation(libs.kelvin.dbtest)
    testImplementation(kelvinLibs.ktor.server.test.host)
    testImplementation(kelvinLibs.junit.jupiter.params)
    testImplementation(kelvinLibs.assertj.core)
    testImplementation(kelvinLibs.nimbus.jose.jwt)
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
