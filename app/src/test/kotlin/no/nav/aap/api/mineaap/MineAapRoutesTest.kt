package no.nav.aap.api.mineaap

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.TokenXTokenGen
import no.nav.aap.api.util.WithFakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@WithFakes
class MineAapRoutesTest : PostgresTestBase() {
    private val tokenx = TokenXTokenGen()
    private val sti = "/mine-aap/saker-med-behandlinger"

    private fun ApplicationTestBuilder.oppsett() {
        application {
            api(
                config = TestConfig.default(),
                datasourceFactory = { dataSource },
                arenaService = Fakes.getArenaService(),
                modiaProducer = Fakes.getKafka(),
                aapHendelseProducer = Fakes.getAapHendelse(),
                pdlGateway = PdlGatewayEmpty(),
                arbeidsoppfølgingProducer = Fakes.getArbeidsoppfølgingHendelse()
            )
        }
    }

    @Test
    fun `godkjent konsument får hente saker`() = testApplication {
        oppsett()

        val response = client.get(sti) {
            bearerAuth(tokenx.generate(clientId = System.getProperty("MINE_AAP_CLIENT_ID")))
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEqualTo("[]")
    }

    @Test
    fun `avviser TokenX-token fra annen konsument`() = testApplication {
        oppsett()

        val response = client.get(sti) {
            bearerAuth(tokenx.generate(clientId = "localhost:annet-team:ukjent-app"))
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `avviser Entra-token uten client_id`() = testApplication {
        oppsett()

        val response = client.get(sti) {
            bearerAuth(AzureTokenGen("test", "test").generate(isApp = true))
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    }
}
