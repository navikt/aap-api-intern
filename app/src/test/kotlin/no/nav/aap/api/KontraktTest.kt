package no.nav.aap.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KontraktTest : PostgresTestBase() {
    /* Det er ikke til å unngå at listen er tom, siden det er behandlingsflyt sin kontrakt som styrere
     * insert-endepunktene. Utenom det, så hadde denne listen ideelt sett vært tom. */
    val unntak = setOf(
        "no.nav.aap.api.SakerRequest",
        "no.nav.aap.api.SakerRequestMeldekortbackend",
        "no.nav.aap.api.kelvin.MeldekortPerioderDTO",
        "no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest",
        "no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt",
        "no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest",
        "no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse",
        "no.nav.aap.komponenter.type.Periode",
    )

    @Test
    fun `schemas kommer kun fra kontrakter`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource,
                        arenaService = fakes.arenaService,
                        pdlGateway = PdlGatewayEmpty(),
                        modiaProducer = fakes.kafka
                    )
                }

                val res = jsonHttpClient.get("/openapi.json") {
                    contentType(ContentType.Application.Json)
                }
                val openapi = jacksonObjectMapper().readTree(res.bodyAsText())
                val schemas = openapi["components"]["schemas"]
                for ((name, _) in schemas.properties()) {
                    if (name in unntak) {
                        continue
                    }

                    /* ideelt sett hadde vi sjekket at klassen er definert i :kontrakt på et eller annet vis. */
                    assertThat(name).satisfiesAnyOf(
                        { it.startsWith("no.nav.aap.api.intern.") },
                        { it.startsWith("no.nav.aap.behandlingsflyt.kontrakt.") },
                    )
                }
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