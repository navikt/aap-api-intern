package no.nav.aap.api.intern

import java.math.BigDecimal
import java.time.LocalDate

public data class NksMeldeperioderResponse(
    val meldeperioder: List<NksMeldeperiode>,
)

public data class NksMeldeperiode(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val fritakMeldeplikt: List<NksDatoperiode>,
    val meldekortMedTimer: List<NksMeldekortMedTimer>,
    val meldekortLevertIMeldeperioden: List<NksMeldekortMedTimer>,
    val timerArbeid: List<NksTimerArbeid>,
    val arbeidsgrad: NksArbeidsgrad,
    val dagsatser: List<NksDagsats>,
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
    val grad: Int,
    val overGrenseVerdi: Boolean,
)

public data class NksDagsats(
    val dagsats: Int,
    val effektivDagsats: Int,
    val gradering: Int,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
)

public data class Meldeplikt(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val status: String,
)
