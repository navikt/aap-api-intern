package no.nav.aap.api.mineaap


data class SakMedBehandlingerVisning(
    val saksnummer: String,
    val sakStatus: SakStatus,
    val behandlinger: List<BehandlingVisningResponse>,
)
enum class SakStatus {
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}
