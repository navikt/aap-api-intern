import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("api-intern.conventions")
    id("io.ktor.plugin") version "3.3.2"
    id("org.flywaydb.flyway") version "11.17.0"
    application
}

application {
    mainClass.set("no.nav.aap.api.AppKt")
}

val komponenterVersjon = "1.0.435"
val ktorVersion = "3.3.2"
val tilgangVersjon = "1.0.155"
val behandlingsflytversjon = "0.0.484"
val arenaOppslagVersjon = "0.0.38"
val resilience4jVersion = "2.3.0"

dependencies {
    implementation(project(":kontrakt"))

    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytversjon")
    implementation("no.nav.aap.arenaoppslag:kontrakt:$arenaOppslagVersjon")

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

    implementation("org.apache.kafka:kafka-clients:4.1.0")
    implementation("org.flywaydb:flyway-core:11.16.0")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    implementation("com.nimbusds:nimbus-jose-jwt:10.6")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.0")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.4.3")

    implementation("com.zaxxer:HikariCP:7.0.2")

    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-kotlin:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-micrometer:${resilience4jVersion}")

    implementation("no.nav:ktor-openapi-generator:1.0.131")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.20.0")
    }
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.4")
    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:jul-to-slf4j:2.0.17")
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}
