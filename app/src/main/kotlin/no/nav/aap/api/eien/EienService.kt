package no.nav.aap.api.eien

import no.nav.aap.api.intern.PeriodeDTO
import no.nav.aap.api.intern.EienSak
import no.nav.aap.api.intern.EienSaksbehandlingsstatus
import no.nav.aap.api.intern.EienYtelsesstatus
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.sak.KelvinStatus
import no.nav.aap.api.sak.SakStatus
import no.nav.aap.api.sak.YtelseStatus
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

internal fun SakStatus.Kelvin.tilEienSak(): EienSak =
    EienSak(
        kilde = Kilde.KELVIN,
        sakId = sakId,
        saksbehandlingsstatus = statusKode.tilEien(),
        ytelsesstatus = ytelsestatus.tilEien(),
        søknadsdatoer = soknadsdatoer,
        vedtaksdato = vedtaksdato,
        perioderMedRett = komprimertePerioderMedRett(),
        foreløpigMaksdato = forelopigMaksdato,
        venteårsak = enhet?.venteAarsak,
    )

private fun SakStatus.Kelvin.komprimertePerioderMedRett(): List<PeriodeDTO> =
    perioder
        .mapNotNull { periode ->
            val fom = periode.fraOgMedDato ?: return@mapNotNull null
            val tom = periode.tilOgMedDato ?: return@mapNotNull null
            Periode(fom, tom)
        }
        .somTidslinje({ it }, { true })
        .komprimer()
        .segmenter()
        .map { PeriodeDTO(it.periode.fom, it.periode.tom) }

private fun KelvinStatus.tilEien(): EienSaksbehandlingsstatus =
    when (this) {
        KelvinStatus.SOKNAD_UNDER_BEHANDLING -> EienSaksbehandlingsstatus.SOKNAD_UNDER_BEHANDLING
        KelvinStatus.REVURDERING_UNDER_BEHANDLING -> EienSaksbehandlingsstatus.REVURDERING_UNDER_BEHANDLING
        KelvinStatus.FERDIGBEHANDLET -> EienSaksbehandlingsstatus.FERDIGBEHANDLET
    }

private fun YtelseStatus.tilEien(): EienYtelsesstatus =
    when (this) {
        YtelseStatus.FOR_VEDTAK -> EienYtelsesstatus.FOR_VEDTAK
        YtelseStatus.LOPENDE -> EienYtelsesstatus.LOPENDE
        YtelseStatus.AVSLUTTET -> EienYtelsesstatus.AVSLUTTET
    }
