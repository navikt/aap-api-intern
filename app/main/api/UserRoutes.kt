package api

import api.arena.ArenaoppslagRestClient
import api.auth.personident
import api.kelvin.KelvinClient
import api.maksimum.KelvinPeriode
import api.maksimum.Vedtak
import api.maksimum.fraKontrakt
import api.perioder.PerioderInkludert11_17Response
import api.perioder.PerioderResponse
import api.postgres.MeldekortPerioderRepository
import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.audience
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

fun NormalOpenAPIRoute.UserApi(
    dataSource: DataSource,
    arena: ArenaoppslagRestClient,
    kelvin: KelvinClient,
    httpCallCounter: PrometheusMeterRegistry
) {

    tag(Tag.TokenX) {
        route("/tokenx") {
            route("/perioder") {
                route("/aktivitetfase").post<CallIdHeader, PerioderInkludert11_17Response, Unit>(
                    info(description = "Henter perioder med vedtak (inkl. aktivitetsfase) for en person innen gitte datointerval")
                ) { callIdHeader, _ ->
                    val person = pipeline.call.personident()

                    httpCallCounter.httpCallCounter(
                        "/tokenx/perioder/aktivitetfase",
                        "person",
                        azpName() ?: "mine-aap"
                    )
                        .increment()
                    val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                        logger.info("CallID ble ikke gitt på kall mot: /perioder/aktivitetfase")
                    }

                    respond(
                        arena.hentPerioderInkludert11_17(
                            callId,
                            InternVedtakRequest(
                                personidentifikator = person,
                                fraOgMedDato = LocalDate.now().minusMonths(1),
                                tilOgMedDato = LocalDate.now().plusMonths(1)
                            )
                        )
                    )
                }
            }
        }
    }

}

private fun OpenAPIPipelineResponseContext<*>.azpName(): String? =
    pipeline.call.principal<JWTPrincipal>()?.let {
        it.payload.claims["azp_name"]?.asString()
    }
