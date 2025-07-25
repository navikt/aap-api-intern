import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("api-intern.conventions")
    id("io.ktor.plugin") version "3.2.2"
    id ("org.flywaydb.flyway") version "9.0.0"
    application
}

application {
    mainClass.set("api.AppKt")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/behandlingsflyt")
        credentials {
            username = "x-access-token"
            password = (project.findProperty("githubPassword")
                ?: System.getenv("GITHUB_PASSWORD")
                ?: System.getenv("GITHUB_TOKEN")
                ?: error("")).toString()
        }
    }
}

val aapLibVersion = "5.0.103"
val komponenterVersjon = "1.0.297"
val ktorVersion = "3.2.2"
val tilgangVersjon = "1.0.98"
val behandlingsflytversjon = "0.0.384"
val kontraktVersjon = "0.0.25"

dependencies {
    implementation(project(":kontrakt"))
    implementation("org.flywaydb:flyway-core:11.10.3")
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
    constraints {
        implementation("io.netty:netty-common:4.2.3.Final")
    }
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")
    implementation("com.nimbusds:nimbus-jose-jwt:10.4")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.2")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.3.10")

    implementation("com.zaxxer:HikariCP:6.3.0")

    implementation("no.nav:ktor-openapi-generator:1.0.118")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.0")
    testImplementation(kotlin("test"))
}


tasks {
    withType<Test> {
        useJUnitPlatform()
    }
    withType<ShadowJar> {
        mergeServiceFiles()
    }
}
