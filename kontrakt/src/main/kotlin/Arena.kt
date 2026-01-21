package no.nav.aap.api.intern

import java.time.LocalDate

public data class PersonHarSignifikantAAPArenaHistorikk(
    val harSignifikantHistorikk: Boolean,
    val signifikanteSaker: List<String> // signifikante Arena-saker, sortert på dato, nyeste først
)

public data class PersonEksistererIAAPArena(
    val eksisterer: Boolean
)

public data class KanBehandleSoknadIKelvin (
    val personidentifikatorer: List<String>,
    val virkningstidspunkt: LocalDate, // datoen søknaden ble mottatt, feks. per post
)