package api.vedtaksdata

import api.TestConfig
import api.api
import api.maksimum.Maksimum
import api.maksimum.UtbetalingMedMer
import api.maksimum.Vedtak
import api.maksimum.VedtakDataKelvin
import api.util.AzureTokenGen
import api.util.Fakes
import api.util.PostgresTestBase
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.api.intern.Periode
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

class VedtaksDataTest : PostgresTestBase() {
    @Test
    fun `kan lagre ned og hente vedtaksdata`() {
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

                val maksimumTestObject = VedtakDataKelvin(
                    fnr = "12345678910",
                    maksimum = Maksimum(
                        vedtak = listOf(
                            Vedtak(
                                vedtaksId = "1234567",
                                dagsats = 222,
                                status = "IVERK",
                                saksnummer = "12345",
                                vedtaksdato = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                periode = Periode(LocalDate.now().minusWeeks(22), LocalDate.now().plusWeeks(10)),
                                rettighetsType = "test",
                                beregningsgrunnlag = 123,
                                barnMedStonad = 1,
                                kildesystem = "Kelvin",
                                samordningsId = null,
                                opphorsAarsak = null,
                                vedtaksTypeKode = "test",
                                vedtaksTypeNavn = "test",
                                utbetaling = listOf(
                                    UtbetalingMedMer(
                                        reduksjon = null,
                                        utbetalingsgrad = 100,
                                        periode = Periode(LocalDate.now().minusWeeks(22), LocalDate.now().minusWeeks(20)),
                                        belop = 2220,
                                        dagsats = 222,
                                        barnetilegg = 270
                                    )
                                )
                            ),
                            Vedtak(
                                vedtaksId = "123456",
                                dagsats = 222,
                                status = "IVERK",
                                saksnummer = "1234",
                                vedtaksdato = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                periode = Periode(LocalDate.now().minusWeeks(22), LocalDate.now().plusWeeks(10)),
                                rettighetsType = "test",
                                beregningsgrunnlag = 123,
                                barnMedStonad = 1,
                                kildesystem = "Kelvin",
                                samordningsId = null,
                                opphorsAarsak = null,
                                vedtaksTypeKode = "test",
                                vedtaksTypeNavn = "test",
                                utbetaling = listOf(
                                    UtbetalingMedMer(
                                        reduksjon = null,
                                        utbetalingsgrad = 100,
                                        periode = Periode(LocalDate.now().minusWeeks(22), LocalDate.now().minusWeeks(20)),
                                        belop = 2220,
                                        dagsats = 222,
                                        barnetilegg = 270
                                    )
                                )
                            )
                        )
                    )
                )

                val res = jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        maksimumTestObject
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)
                assertEquals(countVedtakEntries(), 2)
                assertEquals(countUtbetalingEntries(), 2)


                val insertData2 = jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        VedtakDataKelvin(
                            fnr = "12345678910",
                            maksimum = Maksimum(
                                vedtak = listOf(
                                    Vedtak(
                                        vedtaksId = "1234567",
                                        dagsats = 222,
                                        status = "IVERK",
                                        saksnummer = "12345",
                                        vedtaksdato = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        periode = Periode(LocalDate.now().minusWeeks(22), LocalDate.now().plusWeeks(10)),
                                        rettighetsType = "test",
                                        beregningsgrunnlag = 123,
                                        barnMedStonad = 1,
                                        kildesystem = "Kelvin",
                                        samordningsId = null,
                                        opphorsAarsak = null,
                                        vedtaksTypeKode = "test",
                                        vedtaksTypeNavn = "test",
                                        utbetaling = listOf(
                                            UtbetalingMedMer(
                                                reduksjon = null,
                                                utbetalingsgrad = 100,
                                                periode = Periode(LocalDate.now().minusWeeks(22), LocalDate.now().minusWeeks(20)),
                                                belop = 2220,
                                                dagsats = 222,
                                                barnetilegg = 270
                                            ),
                                            UtbetalingMedMer(
                                                reduksjon = null,
                                                utbetalingsgrad = 100,
                                                periode = Periode(LocalDate.now().minusWeeks(20), LocalDate.now().minusWeeks(18)),
                                                belop = 2220,
                                                dagsats = 222,
                                                barnetilegg = 270
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                }
                assertEquals(HttpStatusCode.OK, insertData2.status)
                assertEquals(countVedtakEntries(), 2)
                assertEquals(countUtbetalingEntries(), 3)

                /*
                val hentDataRes = jsonHttpClient.post("/maksimum") {
                    bearerAuth(azure.generate(false))
                    contentType(ContentType.Application.Json)
                    setBody(
                        InternVedtakRequest(
                            "12345678910",
                            LocalDate.now().minusWeeks(23),
                            LocalDate.now().plusWeeks(11)
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, hentDataRes.status)
                assertEquals(hentDataRes.body<Maksimum>(), maksimumTestObject.maksimum)
                */
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