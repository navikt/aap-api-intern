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
import no.nav.aap.api.intern.MeldekortDetaljerResponse
import no.nav.aap.api.intern.NksMeldeperioderResponse
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.kelvin.KelvinSakService
import no.nav.aap.api.kelvin.MeldekortService
import no.nav.aap.api.kelvin.NksMeldeperioderService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.*
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("NksRoutes")

fun NormalOpenAPIRoute.nksRoutes(
    dataSource: DataSource,
    arenaService: ArenaService,
    pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone(),
) {
    tag(Tag.Meldekort) {
        route("/kelvin/meldekort-detaljer").authorizedPost<CallIdHeader, MeldekortDetaljerResponse, MeldekortDetaljerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_SAAS_PROXY"))
                ) + azpForTokenGenHvisIkkeProd(),
            ), null, null, null,
            info(description = "Henter detaljerte meldekort for en gitt person og evt. begrenset til en gitt periode. Kan kun brukes av NKS."),
            tags(Tag.NKS)
        ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)
            val personIdentifikator = requestBody.personidentifikator

            val meldekortListe = dataSource.transaction { connection ->
                val meldekortService = MeldekortService(connection, pdlGateway, clock)
                meldekortService.hentAlle(
                    personIdentifikator,
                    requestBody.fraOgMedDato,
                    requestBody.tilOgMedDato
                )
                    .map { (meldekort, tilkjentYtelsePerioder) ->
                        meldekort.tilKontrakt(tilkjentYtelsePerioder)
                    }
            }

            tellKelvinKall(pipeline.call.request)

            if (meldekortListe.isEmpty()) {
                logger.info("Fant ingen meldekort for person $personIdentifikator i den angitte perioden")
            }

            val responseBody = MeldekortDetaljerResponse(personIdentifikator, meldekortListe)
            respond(responseBody, HttpStatusCode.OK)
        }

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
            val callId = receiveCall(callIdHeader, pipeline)

            Metrics.antallIdenter("/sakerByFnr", requestBody.personidentifikatorer.size)

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlGateway)
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val kelvinSakService = KelvinSakService(
                        SakStatusRepository(connection),
                        BehandlingsRepository(connection)
                    )

                    kelvinSakService.hentSakStatus(personIdenter)
                }
            val arenaSaker: List<SakStatus> =
                arenaService.hentSaker(callId, requestBody.personidentifikatorer)

            tellKildesystem(kelvinSaker, arenaSaker, "/sakerByFnr")

            respond(arenaSaker + kelvinSaker)
        }
    }
}
