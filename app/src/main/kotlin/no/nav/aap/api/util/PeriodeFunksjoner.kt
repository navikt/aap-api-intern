package no.nav.aap.api.util

fun perioderMedAAp(
    input: List<api.postgres.DatadelingDTO>
): List<no.nav.aap.api.intern.Periode> {
    return input.flatMap { sak ->
        sak.rettighetsTypeTidsLinje.map { segment ->
            no.nav.aap.api.intern.Periode(segment.fom, segment.tom)
        }
    }
}