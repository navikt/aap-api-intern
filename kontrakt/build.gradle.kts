import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

apply(plugin = "maven-publish")
apply(plugin = "java-library")

java {
    withSourcesJar()
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-api-intern")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}