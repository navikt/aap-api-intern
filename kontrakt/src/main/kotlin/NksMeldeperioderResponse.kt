package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.math.BigDecimal
import java.time.LocalDate

public data class NksMeldeperioderResponse(
    @param:Description("Liste av meldeperioder.")
    val meldeperioder: List<NksMeldeperiode>,
)

public data class NksMeldeperiode(
    @param:Description("Fra-dato for meldeperioden.")
    val fraDato: LocalDate,
    @param:Description("Til-dato for meldeperioden. Disse er alltid to uker.")
    val tilDato: LocalDate,
    @param:Description("Perioder med fritak meldeplikt som overlapper med denne meldeperioden.")
    val fritakMeldeplikt: List<NksDatoperiode>,
    @param:Description("Meldekort med timer som overlapper med denne meldeperioden. Merk at for papirmeldekort er det ingen begrensning for at alle timene er innenfor èn meldeperiode.")
    val meldekortMedTimer: List<NksMeldekortMedTimer>,
    @param:Description("Meldekort som er levert i denne meldeperiodee, ikke nødvendigvis med timer for denne perioden. Disse vil oppfylle meldeplikten for denne perioden.")
    val meldekortLevertIMeldeperioden: List<NksMeldekortMedTimer>,
    @param:Description("Timer registrert i denne meldeperioden.")
    val timerArbeid: List<NksTimerArbeid>,
    @param:Description("Arbeidsgrad for denne meldeperioden.")
    val arbeidsgrad: NksArbeidsgrad,
    @param:Description("Dagsatser for denne meldeperioden.")
    val dagsatser: List<NksDagsats>,
    @param:Description("Meldepliktstatuser innenfor denne meldeperioden.")
    val meldeplikt: List<Meldeplikt>,
)

public data class NksDatoperiode(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
)

public data class NksMeldekortMedTimer(
    val journalPostId: String?,
    val mottattDato: LocalDate,
)

public data class NksTimerArbeid(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val timerArbeidet: BigDecimal,
)

public data class NksArbeidsgrad(
    @param:Description("Arbeidsgraden i prosent. Null betyr ikke noe arbeid, mens 100% er full stilling.")
    val grad: Int,
    @param:Description("Om arbeidsgraden er over grenseverdien, slik at utbetaling reduseres til 0.")
    val overGrenseverdi: Boolean,
)

public data class NksDagsats(
    @param:Description("Full dagsats, før reduksjoner.")
    val dagsats: Int,
    @param:Description("Dagsats etter reduksjoner (arbeid, samordning, osv.)")
    val effektivDagsats: Int,
    @param:Description("Mellom 0 og 100 prosent. 100% betyr at full AAP utbetales.")
    val gradering: Int,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)

public data class Meldeplikt(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    @param:Description("IKKE_MELDT_SEG betyr brudd på meldeplikten.")
    val status: String,
)
