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

    implementation(libs.kelvinServer)
    implementation(libs.kelvinInfrastructure)
    implementation(libs.kelvinDbconnect)
    implementation(libs.kelvinDbmigrering)
    implementation(libs.kelvinTidslinje)
    implementation(libs.tilgangPlugin)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.oppgaveApiKontrakt)
    implementation(libs.arenaoppslagKontrakt)

    implementation(libs.ktorSerializationJackson)

    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerCallLoggingJvm)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerMetricsMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorServerCallId)

    implementation(libs.ktorClientAuth)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientJackson)
    implementation(libs.ktorClientLogging)

    implementation(libs.kafkaClients)
    implementation(libs.logback)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.prometheusMetricsTracerInitializer)

    implementation(libs.hikaricp)

    implementation(libs.resilience4jCircuitbreaker)
    implementation(libs.resilience4jKotlin)
    implementation(libs.resilience4jMicrometer)

    implementation(libs.caffeine)

    implementation(libs.kelvinKtorOpenapiGenerator)
    testImplementation(libs.kelvinDbtest)
    testImplementation(libs.ktorServerTestHost)
    constraints {
        implementation(libs.commonsCodec)
    }
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.assertj)
    testImplementation(libs.nimbusJoseJwt)
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}

tasks.register<JavaExec>("runTestApp") {
    mainClass.set("no.nav.aap.api.TestAppKt")
    classpath = sourceSets["test"].runtimeClasspath
}
