package api.afp

import java.time.LocalDate

data class VedtakResponse(val perioder: List<VedtakPeriode>)

data class VedtakPeriode(val fraOgMedDato:LocalDate, val tilOgMedDato:LocalDate?)