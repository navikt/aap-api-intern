package no.nav.aap.api.intern

import java.time.LocalDate

public data class ArenaSakerRequest(
    val personidentifikator: String,
)

public data class ArenaSakOppsummering(
    val sakId: String,
    val lopenummer: Int,
    val aar: Int,
    val antallVedtak: Int,
    val statuskode: String,
    val statusnavn: String,
    val sakstype: String?,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
)

public data class ArenaSakerResponse(
    val saker: List<ArenaSakOppsummering>,
)