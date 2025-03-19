package api.util

import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

fun perioderMedAAp(input:List<DatadelingDTO>, interval:Periode):List<no.nav.aap.api.intern.Periode>{
    return input.flatMap { sak ->
        sak.tilkjent
            .filter { it.gradering > 0 }
            .map { Segment(Periode(it.tilkjentFom, it.tilkjentTom),null) }
            .let (::Tidslinje)
            .filter { interval.overlapper(it.periode) }
            .komprimer()
            .map { no.nav.aap.api.intern.Periode(it.periode.fom, it.periode.tom) }
    }
}