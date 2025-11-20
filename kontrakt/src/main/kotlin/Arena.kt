package no.nav.aap.api.intern

public data class ArenaStatusResponse(
    val kanBehandlesIKelvin: Boolean,
    val nyesteArenaSakId: String? // nyeste Arena-sak for personen, dersom noen relevante finnes
)