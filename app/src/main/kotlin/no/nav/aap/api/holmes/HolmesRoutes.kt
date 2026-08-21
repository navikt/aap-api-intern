package no.nav.aap.api.holmes

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.tags
import io.ktor.http.HttpStatusCode
import no.nav.aap.api.CallIdHeader
import no.nav.aap.api.Metrics
import no.nav.aap.api.Tag
import no.nav.aap.api.azpForTokenGenHvisIkkeProd
import no.nav.aap.api.intern.HolmesArbeidstimerResponse
import no.nav.aap.api.intern.HolmesMeldeperiode
import no.nav.aap.api.intern.HolmesTimerArbeid
import no.nav.aap.api.intern.MeldekortDetaljerRequest
import no.nav.aap.api.kelvin.NksMeldeperioderService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

fun NormalOpenAPIRoute.holmesRoutes(
    dataSource: DataSource,
    pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone(),
) {
    tag(Tag.Holmes) {
        route("/holmes/arbeidstimer").authorizedPost<CallIdHeader, HolmesArbeidstimerResponse, MeldekortDetaljerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_HOLMES_PERSONDATA_API"))
                ) + azpForTokenGenHvisIkkeProd(),
            ), null, null, null,
            info(description = "Henter arbeidstimer per meldeperiode for en gitt person. Kan kun brukes av team Holmes."),
            tags(Tag.Holmes)
        ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)
            val personIdentifikator = requestBody.personidentifikator

            val meldeperioderResponse = dataSource.transaction { connection ->
                NksMeldeperioderService(connection, pdlGateway, clock).hent(
                    personIdentifikator,
                    requestBody.fraOgMedDato,
                    requestBody.tilOgMedDato,
                )
            }

            val responseBody = HolmesArbeidstimerResponse(
                personIdent = personIdentifikator,
                meldeperioder = meldeperioderResponse.meldeperioder.map {
                    HolmesMeldeperiode(
                        periodeFom = it.fraDato,
                        periodeTom = it.tilDato,
                        timerArbeid = it.timerArbeid.map { timerArbeid ->
                            HolmesTimerArbeid(
                                periodeFom = timerArbeid.periodeFom,
                                periodeTom = timerArbeid.periodeTom,
                                timerArbeidet = timerArbeid.timerArbeidet,
                            )
                        },
                    )
                },
            )

            respond(responseBody, HttpStatusCode.OK)
        }
    }
}
