package api.meldekortperioder

import api.kelvin.MeldekortPerioderDTO
import api.util.TestBase
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortPeriodeTest : TestBase() {

    @Test
    fun `kan lagre ned og hente meldekort perioder`() {
        runBlocking {
            val perioder = listOf(
                Periode(LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 15)),
                Periode(LocalDate.ofYearDay(2021, 16), LocalDate.ofYearDay(2021, 31))
            )

            val res = httpClient.post("/api/insert/meldeperioder") {
                bearerAuth(azure.generate(true))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortPerioderDTO(
                        "12345678910",
                        perioder
                    )
                )
            }

            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals(countMeldekortEntries(), 2)

            val meldekortPerioderRes = httpClient.post("/perioder/meldekort") {
                bearerAuth(azure.generate(true))
                contentType(ContentType.Application.Json)
                setBody(InternVedtakRequest("12345678910", LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 31)))
            }

            assert(meldekortPerioderRes.status.isSuccess())
            assertEquals(meldekortPerioderRes.body<List<Periode>>(), perioder)
        }
    }
}