package no.nav.aap.api.mineaap

data class SakMedBehandlingerVisning(
    val saksnummer: String,
    val behandlinger: List<BehandlingVisning>,
)