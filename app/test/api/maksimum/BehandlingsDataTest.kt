package api.maksimum

import kotlin.random.Random

import api.TestConfig
import api.api
import api.kelvin.MeldekortPerioderDTO
import api.util.AzureTokenGen
import api.util.PostgresTestBase
import io.ktor.server.testing.*
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Test
import api.util.Fakes
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

val testObject = DatadelingDTO(
    underveisperiode = listOf(
        UnderveisDTO(
            underveisFom = LocalDate.now().minusYears(2),
            underveisTom = LocalDate.now().minusYears(1),
            meldeperiodeFom = LocalDate.now().minusYears(2),
            meldeperiodeTom = LocalDate.now().minusYears(1),
            utfall = "",
            rettighetsType = RettighetsType.STUDENT,
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
        fnr = listOf("1234678910", "10987654321"),
        opprettetTidspunkt = LocalDateTime.now().minusYears(2),
    ),
    tilkjent = generateSequence(LocalDate.now().minusYears(2)) { it.plusWeeks(2) }
        .takeWhile { it.isBefore(LocalDate.now().minusYears(1)) }
        .map { it to it.plusWeeks(2).minusDays(1) }
        .toList()
        .let { periods ->
            val halfwayPoint = periods.size / 2
            val dagsatsChangePoint = halfwayPoint + Random.nextInt(periods.size - halfwayPoint)

            periods.mapIndexed { index, periode ->
                TilkjentDTO(
                    tilkjentFom = periode.first,
                    tilkjentTom = periode.second,
                    dagsats = if (index >= dagsatsChangePoint) Random.nextInt(100, 200) else Random.nextInt(50, 100),
                    gradering = if (index == halfwayPoint) 0 else Random.nextInt(1, 100),
                    grunnlag = Random.nextInt(1000, 2000),
                    grunnlagsfaktor = Random.nextInt(1, 10),
                    grunnbeløp = Random.nextInt(50000, 100000),
                    antallBarn = Random.nextInt(0, 5),
                    barnetilleggsats = Random.nextInt(0, 1000),
                    barnetillegg = Random.nextInt(0, 500)
                )
            }
        }


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
                        datasource = InitTestDatabase.dataSource
                    )
                }

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
            }
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
}