package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.time.LocalDate

public data class SignifikanteSakerResponse(
    val harSignifikantHistorikk: Boolean,
    val signifikanteSaker: List<String> // signifikante Arena-saker, sortert på dato, nyeste først
)

public data class PersonEksistererIAAPArena(
    @property:Description("True om personen eksisterer i Arena.")
    val eksisterer: Boolean
)

public data class SignifikanteSakerRequest (
    val personidentifikatorer: List<String>,
    val virkningstidspunkt: LocalDate, // datoen søknaden ble mottatt, feks. per post
)

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