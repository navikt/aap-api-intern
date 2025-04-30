package api.meldekortperioder

import api.kelvin.MeldekortPerioderDTO
import api.util.TestBase
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

private val dataSource = InitTestDatabase.freshDatabase()

class MeldekortPeriodeTest : TestBase(dataSource) {
    @Test
    fun `kan lagre ned og hente meldekort perioder`() =
        runBlocking {
            val perioder =
                listOf(
                    Periode(LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 15)),
                    Periode(LocalDate.ofYearDay(2021, 16), LocalDate.ofYearDay(2021, 31)),
                )

            val res =
                httpClient.post("/api/insert/meldeperioder") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        MeldekortPerioderDTO(
                            "12345678910",
                            perioder,
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals(countMeldekortEntries(), 2)

            val meldekortPerioderRes =
                httpClient.post("/perioder/meldekort") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        InternVedtakRequest(
                            "12345678910",
                            LocalDate.ofYearDay(2021, 1),
                            LocalDate.ofYearDay(2021, 31),
                        ),
                    )
                }

            assert(meldekortPerioderRes.status.isSuccess())
            assertEquals(meldekortPerioderRes.body<List<Periode>>(), perioder)
        }
}
