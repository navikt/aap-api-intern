package no.nav.aap.api.mineaap

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
import java.time.LocalDateTime

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
    fun `returnerer egne saker med hver sine behandlinger`() = testApplication {
        val fnr = "20000000001"
        val annenPersonFnr = "20000000002"
        lagreSak(fnr, "MA-1A")
        lagreSak(fnr, "MA-1B")
        lagreSak(annenPersonFnr, "MA-1C")
        lagreBehandling("MA-1A", behandling("ma-1a-behandling"))
        lagreBehandling("MA-1B", behandling("ma-1b-behandling"))
        lagreBehandling("MA-1C", behandling("ma-1c-behandling"))

        val response = hentSaker(fnr)

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val saker = response.lesSaker()
        assertThat(saker.map { it.saksnummer }).containsExactlyInAnyOrder("MA-1A", "MA-1B")
        assertThat(saker.single { it.saksnummer == "MA-1A" }.behandlinger.map { it.behandlingReferanse })
            .containsExactly("ma-1a-behandling")
        assertThat(saker.single { it.saksnummer == "MA-1B" }.behandlinger.map { it.behandlingReferanse })
            .containsExactly("ma-1b-behandling")
    }

    @Test
    fun `returnerer sak med tom behandlingsliste når saken ikke har behandlinger`() = testApplication {
        val fnr = "20000000003"
        lagreSak(fnr, "MA-2")

        val saker = hentSaker(fnr).lesSaker()

        assertThat(saker).hasSize(1)
        assertThat(saker.single().saksnummer).isEqualTo("MA-2")
        assertThat(saker.single().behandlinger).isEmpty()
    }

    @Test
    fun `returnerer tom liste når bruker ikke har saker`() = testApplication {
        val response = hentSaker("20000000004")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.lesSaker()).isEmpty()
    }

    @Test
    fun `finner sak lagret under historisk ident`() = testApplication {
        val nåværendeFnr = "20000000005"
        val historiskFnr = "20000000006"
        lagreSak(historiskFnr, "MA-4")
        lagreBehandling("MA-4", behandling("ma-4-behandling"))

        val pdlGateway = object : IPdlGateway {
            override fun hentAlleIdenterForPerson(personIdent: String) =
                listOf(
                    PdlIdent(ident = nåværendeFnr, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
                    PdlIdent(ident = historiskFnr, historisk = true, gruppe = "FOLKEREGISTERIDENT"),
                )
        }

        val saker = hentSaker(nåværendeFnr, pdlGateway).lesSaker()

        assertThat(saker.map { it.saksnummer }).containsExactly("MA-4")
        assertThat(saker.single().behandlinger.map { it.behandlingReferanse })
            .containsExactly("ma-4-behandling")
    }

    @Test
    fun `serialiserer hendelser med navn som diskriminator`() = testApplication {
        val fnr = "20000000007"
        lagreSak(fnr, "MA-5")
        lagreBehandling(
            "MA-5",
            BehandlingVisningIntern(
                behandlingReferanse = "ma-5-behandling",
                avsluttet = true,
                behandlingType = BehandlingVisningTypeIntern.KLAGE,
                hendelser = listOf(
                    BehandlingHendelseIntern.SøknadMottatt(
                        sistEndret = LocalDateTime.of(2025, 1, 1, 9, 0),
                        progresjon = BehandlingHendelseProgresjonIntern.FULLFØRT,
                    ),
                    BehandlingHendelseIntern.Nay(
                        sistEndret = null,
                        progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                        status = BehandlingHendelseIntern.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                    ),
                ),
            ),
        )

        val tekst = hentSaker(fnr).bodyAsText()

        val behandling = mapper.readTree(tekst).single()["behandlinger"].single()
        assertThat(behandling["behandlingType"].asText()).isEqualTo("KLAGE")
        assertThat(behandling["hendelser"].map { it["navn"].asText() })
            .containsExactly("SØKNAD_MOTTATT", "NAY")

        val hendelser = mapper.readValue<List<SakMedBehandlingerVisning>>(tekst)
            .single().behandlinger.single().hendelser
        assertThat(hendelser[0]).isInstanceOf(BehandlingHendelseResponse.SøknadMottatt::class.java)
        assertThat(hendelser[1]).isEqualTo(
            BehandlingHendelseResponse.Nay(
                sistEndret = null,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            )
        )
    }

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private suspend fun ApplicationTestBuilder.hentSaker(
        fnr: String,
        pdlGateway: IPdlGateway = PdlGatewayEmpty(),
    ): HttpResponse {
        oppsett(pdlGateway)
        return jsonHttpClient.get(sti) {
            bearerAuth(tokenx.generate(clientId = "localhost:aap:innsyn", pid = fnr))
        }
    }

    private suspend fun HttpResponse.lesSaker(): List<SakMedBehandlingerVisning> =
        mapper.readValue(bodyAsText())

    private fun lagreSak(fnr: String, saksnummer: String) {
        dataSource.transaction { connection ->
            SakStatusRepository(connection).lagreSakStatusFraKelvin(
                fnr = fnr,
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
        }
    }

    private fun lagreBehandling(saksnummer: String, behandling: BehandlingVisningIntern) {
        dataSource.transaction { connection ->
            BehandlingVisningRepository(connection).lagreBehandlingVisning(saksnummer, behandling)
        }
    }

    private fun behandling(referanse: String) = BehandlingVisningIntern(
        behandlingReferanse = referanse,
        avsluttet = false,
        behandlingType = BehandlingVisningTypeIntern.SØKNAD,
        hendelser = emptyList(),
    )
}
