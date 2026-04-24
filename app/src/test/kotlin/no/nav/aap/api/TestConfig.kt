package no.nav.aap.api

import no.nav.aap.api.kafka.KafkaConfig

object TestConfig {
    internal val postgres = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:test_db;MODE=PostgreSQL",
    )

    fun default(): AppConfig {
        return AppConfig(
            arenaoppslag = ArenaoppslagConfig(
                proxyBaseUrl = "",
                scope = "api://dev-fss.teamdokumenthandtering.dokarkiv/.default"
            ),
            kafka = KafkaConfig(
                brokers = "localhost:2222",
                truststorePath = "test",
                keystorePath = "test",
                credstorePsw = "test"
            ),
            dbConfig = postgres,
            modia = ModiaConfig(
                topic = "test"
            )
        )
    }

}