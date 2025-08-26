import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("api-intern.conventions")
    id("io.ktor.plugin") version "3.2.3"
    id("org.flywaydb.flyway") version "11.11.2"
    application
}

application {
    mainClass.set("api.AppKt")
}

val komponenterVersjon = "1.0.337"
val ktorVersion = "3.2.3"
val tilgangVersjon = "1.0.114"
val behandlingsflytversjon = "0.0.410"
val kontraktVersjon = "0.0.26"

dependencies {
    implementation(project(":kontrakt"))

    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytversjon")
    implementation("no.nav.aap.arenaoppslag:kontrakt:$kontraktVersjon")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("org.flywaydb:flyway-core:11.11.1")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    implementation("com.nimbusds:nimbus-jose-jwt:10.4.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.3")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.4.1")

    implementation("com.zaxxer:HikariCP:7.0.2")

    implementation("no.nav:ktor-openapi-generator:1.0.122")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.19.0")
    }
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.0")
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
    }
}