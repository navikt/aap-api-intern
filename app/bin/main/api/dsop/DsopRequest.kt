package api.dsop

import java.time.LocalDate


data class DsopRequest(
    val personId: String,
    val periode: Periode,
    val samtykkePeriode: Periode
)

data class Periode(
    val fraDato: LocalDate,
    val tilDato: LocalDate
) {
    fun erDatoIPeriode(dato: LocalDate) = dato in fraDato..tilDato

}