package api.util

import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

fun perioderMedAAp(input:List<DatadelingDTO>, interval:Periode):List<no.nav.aap.api.intern.Periode>{
    return input.flatMap { sak ->
        sak.rettighetsTypeTidsLinje.map { segment ->
            no.nav.aap.api.intern.Periode(segment.fom, segment.tom)
            }
    }
}