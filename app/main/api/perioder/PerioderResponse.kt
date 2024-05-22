package api.perioder

import java.time.LocalDate

data class PerioderResponse(val perioder: List<Periode>)

data class Periode(val fraOgMedDato:LocalDate, val tilOgMedDato:LocalDate?)
