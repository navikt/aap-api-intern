package no.nav.aap.api.intern

import java.time.LocalDate

data class Periode(val fraOgMedDato: LocalDate?, val tilOgMedDato: LocalDate?)

data class SakStatus(
    val sakId: String,
    val statusKode: Status,
    val periode: Periode,
    val kilde:Kilde = Kilde.ARENA
)

enum class Kilde{
    ARENA,
    KELVIN
}

enum class Status{
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,
    UKJENT,
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET
}