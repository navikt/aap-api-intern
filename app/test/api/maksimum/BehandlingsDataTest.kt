package api.maksimum

import kotlin.random.Random

import api.TestConfig
import api.api
import api.arena.ArenaoppslagRestClient
import api.arena.IArenaoppslagRestClient
import api.perioder.PerioderInkludert11_17Response
import api.perioder.PerioderResponse
import api.postgres.mergeTilkjentPeriods
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
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.*
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.type.Periode
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

class BehandlingsDataTest : PostgresTestBase() {

    @Test
    fun `kan lagre ned og hente behandlingsdata`() {
        Fakes().use { fakes ->

            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = InitTestDatabase.dataSource,
                        arenaRestClient = ArenaClient()
                    )
                }
                println("datasource: ${InitTestDatabase.dataSource.connection}")
                val res = jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        testObject
                    )
                }


                assertEquals(HttpStatusCode.OK, res.status)
                val perioder = countTilkjentPerioder()
                assert(perioder > 0)

                val collectRes = jsonHttpClient.post("/maksimum") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }

                assertEquals(HttpStatusCode.OK, collectRes.status)
                assertEquals(4, collectRes.body<Maksimum>().vedtak.size)
            }
        }
    }

    @Test
    fun `kan hente perioder fra vedtaksdata`() {
        val interval = Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(2).plusWeeks(6))
        val result = perioderMedAAp(listOf(testObject), interval = interval)

        assertEquals(2,result.size)
        assertEquals(result, listOf(
            no.nav.aap.api.intern.Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(2).plusWeeks(4).minusDays(1)),
            no.nav.aap.api.intern.Periode(LocalDate.now().minusYears(2).plusWeeks(6), LocalDate.now().minusYears(2).plusWeeks(8).minusDays(1))
        ))
    }
    //@Test
    fun `mergePerioder`(){
        assertEquals(testObjectResult.tilkjent ,mergeTilkjentPeriods(testObject.tilkjent))
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