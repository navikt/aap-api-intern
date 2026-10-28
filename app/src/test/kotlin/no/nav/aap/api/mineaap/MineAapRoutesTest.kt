package no.nav.aap.api.mineaap

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.behandlingsflyt.Periode
import no.nav.aap.api.intern.behandlingsflyt.SakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.pdl.PdlIdent
import no.nav.aap.api.postgres.BehandlingVisningRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.TokenXTokenGen
import no.nav.aap.api.util.WithFakes
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

@WithFakes
class MineAapRoutesTest : PostgresTestBase() {
    private val tokenx = TokenXTokenGen()
    private val sti = "/mine-aap/saker-med-behandlinger"

    private fun ApplicationTestBuilder.oppsett(
        pdlGateway: IPdlGateway = PdlGatewayEmpty(),
    ) {
        application {
            api(
                config = TestConfig.default(),
                datasourceFactory = { dataSource },
                arenaService = Fakes.getArenaService(),
                modiaProducer = Fakes.getKafka(),
                aapHendelseProducer = Fakes.getAapHendelse(),
                pdlGateway = pdlGateway,
                arbeidsoppfølgingProducer = Fakes.getArbeidsoppfølgingHendelse(),
            )
        }
    }

    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() =
            createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    }
                }
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
    @Test
    fun `kobler ikke behandling fra annet fnr selv om saksnummer er likt`() =
        testApplication {
            val nåværendeFnr = "10000000001"
            val historiskFnr = "10000000002"
            val saksnummer = "SAK-1"

            val pdlGateway = object : IPdlGateway {
                override fun hentAlleIdenterForPerson(personIdent: String) =
                    listOf(
                        PdlIdent(
                            ident = nåværendeFnr,
                            historisk = false,
                            gruppe = "FOLKEREGISTERIDENT",
                        ),
                        PdlIdent(
                            ident = historiskFnr,
                            historisk = true,
                            gruppe = "FOLKEREGISTERIDENT",
                        ),
                    )
            }

            dataSource.transaction { connection ->
                SakStatusRepository(connection).lagreSakStatusFraKelvin(
                    fnr = nåværendeFnr,
                    sakStatus = SakStatus(
                        sakId = saksnummer,
                        søknadsdatoer = emptyList(),
                        statusKode = SakstatusFraKelvin.FERDIGBEHANDLET,
                        periode = Periode(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 12, 31),
                        ),
                    ),
                )

                val repository = BehandlingVisningRepository(connection)

                repository.lagreBehandlingVisning(
                    fnr = nåværendeFnr,
                    saksnummer = saksnummer,
                    behandlingVisning = BehandlingVisning(
                        behandlingReferanse = "behandling-nåværende",
                        avsluttet = false,
                        behandlingType = BehandlingVisningType.SØKNAD,
                        hendelser = emptyList(),
                    ),
                )

                repository.lagreBehandlingVisning(
                    fnr = historiskFnr,
                    saksnummer = saksnummer,
                    behandlingVisning = BehandlingVisning(
                        behandlingReferanse = "behandling-historisk",
                        avsluttet = false,
                        behandlingType = BehandlingVisningType.SØKNAD,
                        hendelser = emptyList(),
                    ),
                )
            }

            oppsett(pdlGateway)

            val response = jsonHttpClient.get(sti) {
                bearerAuth(
                    tokenx.generate(
                        clientId = "localhost:aap:innsyn",
                        pid = nåværendeFnr,
                    ),
                )
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val saker = response.body<List<SakMedBehandlingerVisning>>()

            assertThat(saker).hasSize(1)
            assertThat(saker.single().saksnummer).isEqualTo(saksnummer)
            assertThat(
                saker.single().behandlinger.map(BehandlingVisning::behandlingReferanse)
            ).containsExactly("behandling-nåværende")
        }

}
