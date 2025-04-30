package api.maksimum

import api.util.TestBase
import api.util.perioderMedAAp
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.aap.api.intern.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import no.nav.aap.api.intern.Periode as ApiInternPeriode

val testObject =
    DatadelingDTO(
        behandlingsId = 123456789L.toString(),
        behandlingsReferanse = "1234567890987654321",
        underveisperiode =
            listOf(
                UnderveisDTO(
                    underveisFom = LocalDate.now().minusYears(2),
                    underveisTom = LocalDate.now().minusYears(1),
                    meldeperiodeFom = LocalDate.now().minusYears(2),
                    meldeperiodeTom = LocalDate.now().minusYears(1),
                    utfall = "",
                    rettighetsType = RettighetsType.STUDENT.name,
                    avslagsårsak = "",
                ),
            ),
        rettighetsPeriodeFom = LocalDate.now().minusYears(2),
        rettighetsPeriodeTom = LocalDate.now().minusYears(1),
        behandlingStatus = Status.IVERKSETTES,
        vedtaksDato = LocalDate.now().minusYears(2).plusDays(20),
        sak =
            SakDTO(
                saksnummer = "test",
                status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.LØPENDE,
                fnr = listOf("12345678910", "10987654321"),
                opprettetTidspunkt = LocalDateTime.now().minusYears(2),
            ),
        tilkjent =
            listOf(
                TilkjentDTO(
                    tilkjentFom = LocalDate.now().minusYears(2),
                    tilkjentTom =
                        LocalDate
                            .now()
                            .minusYears(2)
                            .plusWeeks(2)
                            .minusDays(1),
                    dagsats = 200,
                    gradering = 100,
                    grunnlag = 2000.toBigDecimal(),
                    grunnlagsfaktor = 2.4.toBigDecimal(),
                    grunnbeløp = 123321.toBigDecimal(),
                    antallBarn = 2,
                    barnetilleggsats = 36.toBigDecimal(),
                    barnetillegg = (36 * 2).toBigDecimal(),
                ),
                TilkjentDTO(
                    tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(2),
                    tilkjentTom =
                        LocalDate
                            .now()
                            .minusYears(2)
                            .plusWeeks(4)
                            .minusDays(1),
                    dagsats = 200,
                    gradering = 100,
                    grunnlag = 2000.toBigDecimal(),
                    grunnlagsfaktor = 2.4.toBigDecimal(),
                    grunnbeløp = 123321.toBigDecimal(),
                    antallBarn = 2,
                    barnetilleggsats = 36.toBigDecimal(),
                    barnetillegg = (36 * 2).toBigDecimal(),
                ),
                TilkjentDTO(
                    tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(4),
                    tilkjentTom =
                        LocalDate
                            .now()
                            .minusYears(2)
                            .plusWeeks(6)
                            .minusDays(1),
                    dagsats = 300,
                    gradering = 0,
                    grunnlag = 2000.toBigDecimal(),
                    grunnlagsfaktor = 2.4.toBigDecimal(),
                    grunnbeløp = 123321.toBigDecimal(),
                    antallBarn = 2,
                    barnetilleggsats = 36.toBigDecimal(),
                    barnetillegg = (36 * 2).toBigDecimal(),
                ),
                TilkjentDTO(
                    tilkjentFom = LocalDate.now().minusYears(2).plusWeeks(6),
                    tilkjentTom =
                        LocalDate
                            .now()
                            .minusYears(2)
                            .plusWeeks(8)
                            .minusDays(1),
                    dagsats = 300,
                    gradering = 100,
                    grunnlag = 2000.toBigDecimal(),
                    grunnlagsfaktor = 2.4.toBigDecimal(),
                    grunnbeløp = 123321.toBigDecimal(),
                    antallBarn = 2,
                    barnetilleggsats = 36.toBigDecimal(),
                    barnetillegg = (36 * 2).toBigDecimal(),
                ),
            ),
        rettighetsTypeTidsLinje =
            listOf(
                RettighetsTypePeriode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().minusYears(1),
                    verdi = RettighetsType.BISTANDSBEHOV.name,
                ),
            ),
    )

private val dataSource = InitTestDatabase.freshDatabase()

class BehandlingsDataTest : TestBase(dataSource) {
    @Test
    fun `kan lagre ned og hente behandlingsdata`() =
        runBlocking {
            val res =
                httpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        testObject,
                    )
                }

            assertEquals(HttpStatusCode.OK, res.status)
            val perioder = countTilkjentPerioder()
            assert(perioder > 0)

            val collectRes =
                httpClient.post("/maksimum") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.now().minusYears(3), LocalDate.now()))
                }

            assertEquals(HttpStatusCode.OK, collectRes.status)
            assertEquals(3, collectRes.body<Maksimum>().vedtak.size)
        }

    @Test
    fun `kan hente perioder fra vedtaksdata`() {
        val result = perioderMedAAp(listOf(testObject))

        assertEquals(1, result.size)
        assertEquals(
            listOf(
                ApiInternPeriode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1)),
            ),
            result,
        )
    }
}
