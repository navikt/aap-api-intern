package api.perioder

import no.nav.aap.api.intern.Periode

data class PerioderResponse(val perioder: List<Periode>)

data class PeriodeInkludert11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)

data class PerioderInkludert11_17Response(
    val perioder: List<PeriodeInkludert11_17>
)

