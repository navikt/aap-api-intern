package no.nav.aap.api.arena

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.ArenaSakMedVedtakResponse
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.nyIdent
import no.nav.aap.api.util.WithFakes
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse as ArenaSakMedVedtakResponseV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakPerson
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WithFakes
class ArenaOppslagTest {
    companion object {

        private val dataSource = TestDataSource()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            dataSource.close()
        }
    }

    fun testWithKtorApp(testBlock: suspend ApplicationTestBuilder.(azure: AzureTokenGen) -> Unit) {
        val config = TestConfig.default()
        val token = AzureTokenGen("test", "test")

        testApplication {
            application {
                api(
                    config = config,
                    datasourceFactory = { dataSource },
                    arenaService = Fakes.getArenaService(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                    arbeidsoppfølgingProducer = Fakes.getArbeidsoppfølgingHendelse()
                )
            }
            testBlock(token)
        }
    }

    @Test
    fun `kan kalle på aap-eksisterer`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/aap/eksisterer") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequest(personidentifikatorer = listOf("12345678910")))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<PersonEksistererIAAPArena>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.eksisterer).isFalse // forventet respons fra MockedArenaClient
        }
    }

    @Test
    fun `kan hente saker for person med saker`() {
        val ident = nyIdent()
        Fakes.getArenaGateway().leggTilSakerForPerson(
            ident,
            listOf(
                ArenaSakOppsummeringKontrakt(
                    sakId = "1",
                    lopenummer = 1,
                    aar = 2021,
                    antallVedtak = 1,
                    statuskode = "AKTIV",
                    statusnavn = "Aktiv",
                    sakstype = "Arbeidsavklaringspenger",
                    regDato = LocalDate.of(2022, 2, 2),
                    avsluttetDato = null,
                )
            )
        )
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = ident))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<SakerResponse>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.saker).extracting("sakId").containsExactly("1")
        }
    }

    @Test
    fun `kan hente saker for person uten saker`() {
        val ident = nyIdent()
        Fakes.getArenaGateway().leggTilSakerForPerson(ident, emptyList())
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = ident))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<SakerResponse>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.saker).isEmpty()
        }
    }

    @Test
    fun `kan hente saker for person som ikke finnes`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = "finnes ikke"))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<SakerResponse>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.saker).isEmpty()
        }
    }

    @Test
    fun `hentSakerForPerson returnerer 400 når body mangler`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
            }
            assertThat(res.status.value).isGreaterThanOrEqualTo(400)
        }
    }

    @Test
    fun `hentSakerForPerson returnerer 401 uten token`() {
        testWithKtorApp { _ ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = "12345678910"))
            }
            assertThat(res.status).isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun `kan hente Arena-sak med vedtak`() {
        val ident = nyIdent()
        val sakId = leggTilSakMedVedtak(ident)
        testWithKtorApp { token ->
            val res = jsonHttpClient.get("/arena/sak/$sakId") {
                bearerAuth(token.generate(isApp = true))
            }
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<ArenaSakMedVedtakResponse>()
            assertThat(parsedBody.sakId).isEqualTo(sakId)
            assertThat(parsedBody.person.fodselsnummer).isEqualTo(ident)
        }
    }

    @Test
    fun `hentArenaSakMedVedtak returnerer 404 når sak ikke finnes`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.get("/arena/sak/finnes-ikke") {
                bearerAuth(token.generate(isApp = true))
            }
            assertThat(res.status).isEqualTo(HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `hentArenaSakMedVedtak returnerer 401 uten token`() {
        testWithKtorApp { _ ->
            val res = jsonHttpClient.get("/arena/sak/1")
            assertThat(res.status).isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun `hentArenaSakMedVedtak returnerer 403 når OBO-token ikke har tilgang til person`() {
        val sakId = leggTilSakMedVedtak(nyIdent())
        Fakes.withTilgangNektet {
            testWithKtorApp { token ->
                val res = jsonHttpClient.get("/arena/sak/$sakId") {
                    bearerAuth(token.generate(isApp = false))
                }
                assertThat(res.status).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    private fun leggTilSakMedVedtak(fodselsnummer: String): String {
        val sakId = UUID.randomUUID().toString()
        Fakes.getArenaGateway().leggTilSakMedVedtak(
            ArenaSakMedVedtakResponseV1(
                sakId = sakId,
                opprettetAar = 2022,
                lopenr = 1,
                person = ArenaSakPerson(
                    personId = 1,
                    fodselsnummer = fodselsnummer,
                    fornavn = "Test",
                    etternavn = "Testesen",
                ),
                statuskode = "AKTIV",
                statusnavn = "Aktiv",
                registrertDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                avsluttetDato = null,
                vedtak = emptyList(),
            )
        )
        return sakId
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
}
