package no.nav.aap.api.mineaap

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.api.hentAllePersonidenter
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingVisningRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.util.auth.authorizedClientIds
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.server.auth.personBruker
import javax.sql.DataSource

fun NormalOpenAPIRoute.mineAaapApi(
    dataSource: DataSource,
    pdlGateway: IPdlGateway,
) {
    route("/mine-aap") {
        authorizedClientIds(
            requiredConfigForKey("MINE_AAP_CLIENT_ID")
        )
        route("/saker-med-behandlinger") {
            get<Unit, List<SakMedBehandlingerVisning>> {
                val personIdenter = hentAllePersonidenter(listOf(personBruker().pid), pdlGateway)
                val sakerMedeBhandlingerVisning = dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    val behandlingVisningRepository = BehandlingVisningRepository(connection)
                    val saker = personIdenter.flatMap(sakStatusRepository::hentSakStatus)
                    saker.map { sakStatus ->
                        SakMedBehandlingerVisning(
                            saksnummer = sakStatus.sakId,
                            sakStatus = when (sakStatus.statusKode){
                                SakstatusFraKelvin.FERDIGBEHANDLET -> SakStatus.FERDIGBEHANDLET
                                SakstatusFraKelvin.REVURDERING_UNDER_BEHANDLING -> SakStatus.REVURDERING_UNDER_BEHANDLING
                                SakstatusFraKelvin.SOKNAD_UNDER_BEHANDLING -> SakStatus.SOKNAD_UNDER_BEHANDLING
                            },
                            behandlinger = behandlingVisningRepository
                                .hentBehandlingVisninger(saksnummer = sakStatus.sakId)
                                .map { it.tilResponse() },
                        )
                    }
                }
                respond(sakerMedeBhandlingerVisning)
            }
        }
    }
}
