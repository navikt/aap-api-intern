package api.perioder

import java.time.LocalDate

data class PerioderResponse(val perioder: List<Periode>)

data class PeriodeInkludert11_17(
    val periode: Periode,
    val aktivitetsfaseKode: String,
    val aktivitetsfaseNavn: String
)

data class PerioderInkludert11_17Response(
    val perioder: List<PeriodeInkludert11_17>
)

data class Periode(val fraOgMedDato:LocalDate?, val tilOgMedDato:LocalDate?)


data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: VedtakStatus,
    val periode: Periode,
    val kilde:String = "ARENA"
)

enum class VedtakStatus{
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,
    UKJENT
}