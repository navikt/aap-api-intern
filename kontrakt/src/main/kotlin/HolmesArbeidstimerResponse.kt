package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.math.BigDecimal
import java.time.LocalDate

public data class HolmesArbeidstimerResponse(
    @param:Description("Personidentifikator (fnr/dnr) for personen.")
    val personIdent: String,
    @param:Description("Liste av meldeperioder med registrerte arbeidstimer.")
    val meldeperioder: List<HolmesMeldeperiode>,
)

public data class HolmesMeldeperiode(
    @param:Description("Fra-dato for meldeperioden.")
    val periodeFom: LocalDate,
    @param:Description("Til-dato for meldeperioden. Disse er alltid to uker.")
    val periodeTom: LocalDate,
    @param:Description("Timer registrert i denne meldeperioden.")
    val timerArbeid: List<HolmesTimerArbeid>,
)

public data class HolmesTimerArbeid(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val timerArbeidet: BigDecimal,
)
