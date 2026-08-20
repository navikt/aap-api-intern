package no.nav.aap.api.arena

import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak

object ArenaVedtakFilter {
    fun filtrerUgyldigeVedtak(vedtak: List<Vedtak>): List<Vedtak> {
        val ugyldigeVedtak = vedtak.filter {
            val fraOgMedDato = it.periode.fraOgMedDato
            fraOgMedDato != null && it.periode.tilOgMedDato != null && fraOgMedDato > it.periode.tilOgMedDato
        }

        val ugyldigeVedtakId = ugyldigeVedtak.map { it.vedtaksId }.toSet()

        return vedtak
            .filter {
                val fraOgMedDato = it.periode.fraOgMedDato
                val relatertVedtak = it.relatertVedtak.toString()
                (fraOgMedDato == null || it.periode.tilOgMedDato == null || fraOgMedDato <= it.periode.tilOgMedDato)
                        && (relatertVedtak !in ugyldigeVedtakId)
            }
    }
}