package api.util

import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO

fun perioderMedAAp(
    input: List<DatadelingDTO>
): List<no.nav.aap.api.intern.Periode> {
    return input.flatMap { sak ->
        sak.rettighetsTypeTidsLinje.map { segment ->
            no.nav.aap.api.intern.Periode(segment.fom, segment.tom)
        }
    }
}