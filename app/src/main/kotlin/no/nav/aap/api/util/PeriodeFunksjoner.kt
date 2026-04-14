package no.nav.aap.api.util

import no.nav.aap.api.intern.Periode
import no.nav.aap.api.postgres.BehandlingData

fun perioderMedAAp(input: List<BehandlingData>): List<Periode> {
    return input.flatMap { sak ->
        sak.rettighetsTypeTidsLinje.map { segment ->
            Periode(segment.fom, segment.tom)
        }
    }
}