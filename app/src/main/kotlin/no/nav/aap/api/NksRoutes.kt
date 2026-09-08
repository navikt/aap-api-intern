package no.nav.aap.api

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.responseDescription
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.tags
import io.ktor.http.HttpStatusCode
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.intern.MeldekortDetaljerRequest
import no.nav.aap.api.intern.NksMeldeperioderResponse
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.kelvin.KelvinSakService
import no.nav.aap.api.kelvin.NksMeldeperioderService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.sak.tilKontrakt
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.time.Clock
import java.util.*
import javax.sql.DataSource
import no.nav.aap.api.sak.SakStatus as DomeneSakStatus

fun NormalOpenAPIRoute.nksRoutes(
    dataSource: DataSource,
    arenaService: ArenaService,
    pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone(),
) {
    tag(Tag.Meldekort) {

        route("/nks/meldeperioder").authorizedPost<CallIdHeader, NksMeldeperioderResponse, MeldekortDetaljerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_SAAS_PROXY"))
                ) + azpForTokenGenHvisIkkeProd(),
            ), null, null, null,
            info(description = "Henter meldeperioder med meldeplikt, arbeid, meldekort og dagsatser for NKS."),
            tags(Tag.NKS)
        ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)
            val personIdentifikator = requestBody.personidentifikator

            val responseBody = dataSource.transaction { connection ->
                NksMeldeperioderService(connection, pdlGateway, clock).hent(
                    personIdentifikator,
                    requestBody.fraOgMedDato,
                    requestBody.tilOgMedDato,
                )
            }

            tellKelvinKall(pipeline.call.request)
            respond(responseBody, HttpStatusCode.OK)
        }
    }

    tag(Tag.Saker) {
        route("/sakerByFnr").authorizedPost<CallIdHeader, List<SakStatus>, SakerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_SAAS_PROXY"))
                ) + azpForTokenGenHvisIkkeProd(),
            ),
            null,
            null,
            null,
            info(description = "Endepunkt ment kun for NKS. Henter saker for en person."),
            responseDescription(description = "Liste med saker, potensielt fra både Arena og Kelvin. `enhet` er alltid null fra Arena."),
            tags(Tag.NKS)
        ) { callIdHeader, requestBody ->
            val callId = receiveCall(callIdHeader)

            Metrics.antallIdenter("/sakerByFnr", requestBody.personidentifikatorer.size)

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlGateway)
            val kelvinSaker: List<DomeneSakStatus.Kelvin> =
                dataSource.transaction { connection ->
                    val kelvinSakService = KelvinSakService(
                        SakStatusRepository(connection),
                        BehandlingsRepository(connection)
                    )

                    kelvinSakService.hentSakStatus(personIdenter)
                }
            val arenaSaker: List<DomeneSakStatus.Arena> =
                arenaService.hentSaker(callId, requestBody.personidentifikatorer)

            tellKildesystem(kelvinSaker, arenaSaker, "/sakerByFnr")

            respond((arenaSaker + kelvinSaker).map { it.tilKontrakt() })
        }
    }
}
