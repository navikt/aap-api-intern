package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.time.LocalDate

public data class SignifikanteSakerResponse(
    val harSignifikantHistorikk: Boolean,
    val signifikanteSaker: List<String> // signifikante Arena-saker, sortert på dato, nyeste først
)

public data class PersonEksistererIAAPArena(
    @property:Description("True om personen eksister i Arena.")
    val eksisterer: Boolean
)

public data class SignifikanteSakerRequest (
    val personidentifikatorer: List<String>,
    val virkningstidspunkt: LocalDate, // datoen søknaden ble mottatt, feks. per post
)