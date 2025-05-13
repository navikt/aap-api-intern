package api.maksimum

import api.TestConfig
import api.api
import api.util.*
import io.ktor.server.testing.*
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.aap.api.intern.Maksimum
import no.nav.aap.api.intern.Medium
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.*
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

val testObject = DatadelingDTO(
    behandlingsId = 123456789L.toString(),
    behandlingsReferanse = "1234567890987654321",
    underveisperiode = listOf(
        UnderveisDTO(
            underveisFom = LocalDate.now().minusYears(2),
            underveisTom = LocalDate.now().minusYears(1),
            meldeperiodeFom = LocalDate.now().minusYears(2),
            meldeperiodeTom = LocalDate.now().minusYears(1),
            utfall = "",
            rettighetsType = RettighetsType.STUDENT.name,
            avslagsårsak = ""
        )
    ),
    rettighetsPeriodeFom = LocalDate.now().minusYears(2),
    rettighetsPeriodeTom = LocalDate.now().minusYears(1),
    behandlingStatus = Status.IVERKSETTES,
    vedtaksDato = LocalDate.now().minusYears(2).plusDays(20),
    sak = SakDTO(
        saksnummer = "test",
        status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.LØPENDE,
        fnr = listOf("12345678910", "10987654321"),
        opprettetTidspunkt = LocalDateTime.now().minusYears(2),
    ),
    tilkjent = listOf(
        TilkjentDTO(
            tilkjentFom = LocalDate.now().minusYears(2),
            tilkjentTom = LocalDate.now().minusYears(2).plusWeeks(2).minusDays(1),
            dagsats = 200,
            gradering = 100,
            grunnlag = 2000.toBigDecimal(),
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 2,
            barnetilleggsats = 36.toBigDecimal(),
            barnetillegg = (36 * 2).toBigDecimal()
        ),
        TilkjentDTO(
            tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(2),
            tilkjentTom = LocalDate.now().minusYears(2).plusWeeks(4).minusDays(1),
            dagsats = 200,
            gradering = 100,
            grunnlag = 2000.toBigDecimal(),
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 2,
            barnetilleggsats = 36.toBigDecimal(),
            barnetillegg = (36 * 2).toBigDecimal()
        )
        ,TilkjentDTO(
            tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(4),
            tilkjentTom = LocalDate.now().minusYears(2).plusWeeks(6).minusDays(1),
            dagsats = 300,
            gradering = 0,
            grunnlag = 2000.toBigDecimal(),
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 2,
            barnetilleggsats = 36.toBigDecimal(),
            barnetillegg = (36 * 2).toBigDecimal()
        ),
        TilkjentDTO(
            tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(6),
            tilkjentTom = LocalDate.now().minusYears(2).plusWeeks(8).minusDays(1),
            dagsats = 300,
            gradering = 100,
            grunnlag = 2000.toBigDecimal(),
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 2,
            barnetilleggsats = 36.toBigDecimal(),
            barnetillegg = (36 * 2).toBigDecimal()
        )
    ),
    rettighetsTypeTidsLinje = listOf(RettighetsTypePeriode(
        fom = LocalDate.now().minusYears(2),
        tom = LocalDate.now().minusYears(1),
        verdi = RettighetsType.BISTANDSBEHOV.name,
    ))
)

val testObjectResult= DatadelingDTO(
    behandlingsId = 123456789L.toString(),
    behandlingsReferanse = "1234567890987654321",
    underveisperiode = listOf(
        UnderveisDTO(
            underveisFom = LocalDate.now().minusYears(2),
            underveisTom = LocalDate.now().minusYears(1),
            meldeperiodeFom = LocalDate.now().minusYears(2),
            meldeperiodeTom = LocalDate.now().minusYears(1),
            utfall = "",
            rettighetsType = RettighetsType.STUDENT.name,
            avslagsårsak = ""
        )
    ),
    rettighetsPeriodeFom = LocalDate.now().minusYears(2),
    rettighetsPeriodeTom = LocalDate.now().minusYears(1),
    behandlingStatus = Status.IVERKSETTES,
    vedtaksDato = LocalDate.now().minusYears(2).plusDays(20),
    sak = SakDTO(
        saksnummer = "test",
        status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.LØPENDE,
        fnr = listOf("12345678910", "10987654321"),
        opprettetTidspunkt = LocalDateTime.now().minusYears(2),
    ),
    tilkjent = listOf(
        TilkjentDTO(
            tilkjentFom = LocalDate.now().minusYears(2),
            tilkjentTom = LocalDate.now().minusYears(2).plusWeeks(4).minusDays(1),
            dagsats = 200,
            gradering = 100,
            grunnlag = 2000.toBigDecimal(),
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 2,
            barnetilleggsats = 36.toBigDecimal(),
            barnetillegg = (36 * 2).toBigDecimal()
        ),
        TilkjentDTO(
            tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(6),
            tilkjentTom = LocalDate.now().minusYears(2).plusWeeks(8).minusDays(1),
            dagsats = 300,
            gradering = 100,
            grunnlag = 2000.toBigDecimal(),
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 2,
            barnetilleggsats = 36.toBigDecimal(),
            barnetillegg = (36 * 2).toBigDecimal()
        )
    ),
    rettighetsTypeTidsLinje = listOf(RettighetsTypePeriode(
        fom = LocalDate.now().minusYears(2),
        tom = LocalDate.now().minusYears(1),
        verdi = RettighetsType.BISTANDSBEHOV.name,
    ))
)

val dataSource = InitTestDatabase.freshDatabase()

class BehandlingsDataTest : PostgresTestBase(dataSource) {

    @Test
    fun `kan lagre ned og hente maksimum`() {
        Fakes().use { fakes ->

            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource,
                        arenaRestClient = ArenaClient()
                    )
                }
                println("datasource: ${dataSource.connection}")
                val res = jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        testObject
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)
                val perioder = countTilkjentPerioder()
                assert(perioder > 0)

                val maksimumResponseM2m = jsonHttpClient.post("/maksimum") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }

                assertEquals(HttpStatusCode.OK, maksimumResponseM2m.status)
                assertEquals(3, maksimumResponseM2m.body<Maksimum>().vedtak.size)

                val maksimumResponseObo = jsonHttpClient.post("/maksimum") {
                    bearerAuth(OidcToken(azure.generate(isApp = false)).token())
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }
                assertEquals(HttpStatusCode.OK, maksimumResponseObo.status)
            }
        }
    }

    @Test
    fun `kan lagre ned og hente perioder`() {
        Fakes().use { fakes ->

            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource,
                        arenaRestClient = ArenaClient()
                    )
                }

                jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        testObject
                    )
                }

                val perioderResponseObo = jsonHttpClient.post("/perioder") {
                    bearerAuth(OidcToken(azure.generate(isApp = false)).token())
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }

                assertEquals(HttpStatusCode.OK, perioderResponseObo.status)
                assertEquals(perioderResponseObo.body<PerioderResponse>().perioder, perioderMedAAp(listOf(testObject)))

                val perioderResponseM2m = jsonHttpClient.post("/perioder") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }
                assertEquals(HttpStatusCode.OK, perioderResponseM2m.status)
            }
        }
    }

    @Test
    fun `kan lagre ned og hente medium`() {
        Fakes().use { fakes ->

            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource,
                        arenaRestClient = ArenaClient()
                    )
                }

                jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        testObject
                    )
                }

                val mediumResponseM2m = jsonHttpClient.post("/maksimumUtenUtbetaling") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }
                assertEquals(HttpStatusCode.OK, mediumResponseM2m.status)
                assertEquals(3, mediumResponseM2m.body<Medium>().vedtak.size)

                val mediumResponseObo = jsonHttpClient.post("/maksimumUtenUtbetaling") {
                    bearerAuth(OidcToken(azure.generate(isApp = false)).token())
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }
                assertEquals(HttpStatusCode.OK, mediumResponseObo.status)
            }
        }
    }

    @Test
    fun `kan hente perioder fra vedtaksdata`() {
        val result = perioderMedAAp(listOf(testObject))

        assertEquals(1,result.size)
        assertEquals(listOf(
            no.nav.aap.api.intern.Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1))
        ),
            result)
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