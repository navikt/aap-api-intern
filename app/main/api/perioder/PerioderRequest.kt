package api.perioder

import java.time.LocalDate

data class PerioderRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)
