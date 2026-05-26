package no.nav.aap.api.util

import no.nav.aap.api.intern.Periode
import no.nav.aap.api.kelvin.Behandling

fun perioderMedAAp(input: List<Behandling>): List<Periode> {
    return input.flatMap { sak ->
        sak.rettighetsTypePerioder.map { segment ->
            Periode(segment.fom, segment.tom)
        }
    }
}