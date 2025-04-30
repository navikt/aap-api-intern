package api.meldekortperioder

import api.kelvin.SakStatusKelvin
import api.util.TestBase
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakStatusKelvinTest : TestBase() {

    @Test
    fun `kan lagre ned sak status`() {
        runBlocking {
            val res = httpClient.post("/api/insert/sakStatus") {
                bearerAuth(azure.generate(true))
                contentType(ContentType.Application.Json)
                setBody(
                    SakStatusKelvin(
                        ident = "12345678910",
                        status = api.kelvin.SakStatus(
                            sakId = "1234",
                            statusKode = no.nav.aap.arenaoppslag.kontrakt.intern.Status.IVERK,
                            periode = Periode(
                                fom = LocalDate.ofYearDay(2021, 1),
                                tom = LocalDate.ofYearDay(
                                    2021, 31
                                )
                            ),
                            kilde = no.nav.aap.api.intern.Kilde.KELVIN
                        )
                    )
                )
            }

            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals(countSaker(), 1)
        }
    }
}