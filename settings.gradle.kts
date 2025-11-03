plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    "app", "kontrakt"
)

val githubPassword: String? by settings

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/behandlingsflyt")
            credentials {
                username = "x-access-token"
                password = (githubPassword
                    ?: System.getenv("GITHUB_PASSWORD")
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: error("GITHUB_TOKEN not set"))
            }
        }
        mavenLocal()
    }
}
