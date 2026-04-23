package no.nav.aap.api.dsop

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import java.time.Clock
import javax.sql.DataSource
import no.nav.aap.api.CallIdHeader
import no.nav.aap.api.Metrics
import no.nav.aap.api.intern.DsopMeldekortRespons
import no.nav.aap.api.intern.DsopRequest
import no.nav.aap.api.intern.DsopResponse
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.sjekkTilgangTilPerson
import no.nav.aap.api.somDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory

fun NormalOpenAPIRoute.dsopRoutes(
    dataSource: DataSource,
    pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone(),
) {
    val logger = LoggerFactory.getLogger(javaClass)
    route("/vedtak").post<CallIdHeader, DsopResponse, DsopRequest>(
        info(
            description = """Henter ut vedtaksdata for en person for DSOP.
                        Verdier som mangler er ikke tilgjengelige fra Kelvin.
                    """.trimMargin(),
        )
    ) { _, requestBody ->
        logger.info("Henter vedtak fra DSOP")
        Metrics.httpRequestTeller(pipeline.call)
        val uttrekksperiode = Periode(requestBody.fomDato, requestBody.tomDato)

        sjekkTilgangTilPerson(requestBody.personIdent, token())

        val kelvinVedtak = dataSource.transaction { connection ->
            val dsopService = DsopService(connection, pdlGateway, clock)
            dsopService.hentDsopVedtak(requestBody.personIdent, uttrekksperiode)
        }

        respond(
            DsopResponse(
                uttrekksperiode.somDTO,
                kelvinVedtak
            )
        )
    }

    route("/vedtak-test").post<CallIdHeader, DsopResponse, DsopRequest>(
        info(
            description = """Midlertidig endepunkt, kommer til å bli slettet.""".trimMargin(),
        )
    ) { _, requestBody ->
        require(!Miljø.erProd()) { "ikke tilgjengelig i prod" }
        logger.info("Henter vedtak fra DSOP")
        Metrics.httpRequestTeller(pipeline.call)
        val uttrekksperiode = Periode(requestBody.fomDato, requestBody.tomDato)

        sjekkTilgangTilPerson(requestBody.personIdent, token())

        val kelvinVedtak = dataSource.transaction { connection ->
            val dsopService = DsopService(connection, pdlGateway, clock)
            dsopService.hentDsopVedtakNy(requestBody.personIdent, uttrekksperiode)
        }

        respond(
            DsopResponse(
                uttrekksperiode.somDTO,
                kelvinVedtak
            )
        )
    }
    route("/meldekort").post<CallIdHeader, DsopMeldekortRespons, DsopRequest>(
        info(
            description = """Henter ut meldekort for bruker for DSOP API."""
        )
    ) { _, requestBody ->
        Metrics.httpRequestTeller(pipeline.call)
        val uttrekksperiode = Periode(requestBody.fomDato, requestBody.tomDato)

        sjekkTilgangTilPerson(requestBody.personIdent, token())

        val meldekortListe = dataSource.transaction { connection ->
            val dsopService = DsopService(connection, pdlGateway, clock)
            dsopService.hentDsopMeldekort(requestBody.personIdent, uttrekksperiode)
        }

        respond(
            DsopMeldekortRespons(
                uttrekksperiode.somDTO,
                meldekortListe
            )
        )
    }
}