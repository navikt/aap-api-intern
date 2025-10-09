@file:Suppress("ClassName")

package no.nav.aap.api.intern

public data class PerioderResponse(val perioder: List<Periode>)

public data class PeriodeInkludert11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)

public data class PerioderInkludert11_17Response(
    val perioder: List<PeriodeInkludert11_17>
)

