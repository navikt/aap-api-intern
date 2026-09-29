package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.math.BigDecimal
import java.time.LocalDate

public data class NksMeldeperioderResponse(
    @property:Description("Liste av meldeperioder.")
    val meldeperioder: List<NksMeldeperiode>,
)

public data class NksMeldeperiode(
    @property:Description("Fra-dato for meldeperioden.")
    val fraDato: LocalDate,
    @property:Description("Til-dato for meldeperioden. Disse er alltid to uker.")
    val tilDato: LocalDate,
    @property:Description("Perioder med fritak meldeplikt som overlapper med denne meldeperioden.")
    val fritakMeldeplikt: List<NksDatoperiode>,
    @property:Description("Meldekort med timer som overlapper med denne meldeperioden. Merk at for papirmeldekort er det ingen begrensning for at alle timene er innenfor èn meldeperiode.")
    val meldekortMedTimer: List<NksMeldekortMedTimer>,
    @property:Description("Meldekort som er levert i denne meldeperiodee, ikke nødvendigvis med timer for denne perioden. Disse vil oppfylle meldeplikten for denne perioden.")
    val meldekortLevertIMeldeperioden: List<NksMeldekortMedTimer>,
    @property:Description("Timer registrert i denne meldeperioden.")
    val timerArbeid: List<NksTimerArbeid>,
    @property:Description("Arbeidsgrad for denne meldeperioden.")
    val arbeidsgrad: NksArbeidsgrad,
    @property:Description("Dagsatser for denne meldeperioden.")
    val dagsatser: List<NksDagsats>,
    @property:Description("Meldepliktstatuser innenfor denne meldeperioden.")
    val meldeplikt: List<Meldeplikt>,
    @property:Description("Årsaker til redusert utbetaling (ikke inkludert samordning). BRUDD_PAA_MELDEPLIKT: en av dagene i meldeperioden inneholder brudd på meldeplikten. ARBEID_OVER_GRENSEVERDI: arbeidet mer enn 60% (unntak for arbeidsopptrapping). ARBEID: personen har arbeidet mer enn 0%.")
    val aarsakerTilReduksjon: List<ÅrsakTilReduksjon>,
)

public enum class ÅrsakTilReduksjon {
    /**
     * Hvis en av dagene i denne meldeperioden inneholder brudd på meldeplikten.
     */
    BRUDD_PAA_MELDEPLIKT,

    /**
     * Hvis det er arbeidet mer enn 60% i denne meldeperioden (unntak hvis mottaker er på arbeidsopptrapping).
     */
    ARBEID_OVER_GRENSEVERDI,

    /**
     * Hvis personen har arbeidet mer enn 0%.
     */
    ARBEID,
}

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
    @property:Description("Arbeidsgraden i prosent. Null betyr ikke noe arbeid, mens 100% er full stilling.")
    val grad: Int,
    @property:Description("Om arbeidsgraden er over grenseverdien, slik at utbetaling reduseres til 0.")
    val overGrenseverdi: Boolean,
)

public data class NksDagsats(
    @property:Description("Full dagsats, før reduksjoner.")
    val dagsats: Int,
    @property:Description("Dagsats etter reduksjoner (arbeid, samordning, osv.)")
    val effektivDagsats: Int,
    @property:Description("Mellom 0 og 100 prosent. 100% betyr at full AAP utbetales.")
    val gradering: Int,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)

public data class Meldeplikt(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    @property:Description("IKKE_MELDT_SEG betyr brudd på meldeplikten.")
    val status: String,
)
