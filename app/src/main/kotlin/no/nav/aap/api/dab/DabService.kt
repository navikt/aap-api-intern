package no.nav.aap.api.dab

import no.nav.aap.api.intern.DabSak
import no.nav.aap.api.sak.SakStatus
import no.nav.aap.api.sak.tilKontrakt

internal fun List<SakStatus.Kelvin>.kelvinTilDabSaker(): List<DabSak.Kelvin> =
    map { sak ->
        DabSak.Kelvin(
            sakId = sak.sakId,
            statuskode = sak.statusKode.tilKontrakt(),
            perioder = sak.perioder.map { it.tilKontrakt() },
            ytelsesstatus = sak.ytelsestatus.tilKontrakt(),
        )
    }

internal fun List<SakStatus.Arena>.arenaTilDabSaker(): List<DabSak.Arena> =
    distinctBy(SakStatus.Arena::sakId).map { sak ->
        DabSak.Arena(
            sakId = sak.sakId,
            statusKode = sak.statusKode.tilKontrakt(),
            periode = sak.periode.tilKontrakt(),
        )
    }
