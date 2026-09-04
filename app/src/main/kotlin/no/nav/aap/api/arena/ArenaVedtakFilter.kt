package no.nav.aap.api.arena

import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak
import java.time.LocalDate

object ArenaVedtakFilter {
    fun filtrerUgyldigeVedtak(vedtak: List<Vedtak>): List<Vedtak> =
        filtrerUgyldige(
            vedtak = vedtak,
            id = { it.vedtaksId },
            relatertVedtak = { it.relatertVedtak },
            fraOgMedDato = { it.periode.fraOgMedDato },
            tilOgMedDato = { it.periode.tilOgMedDato },
        )

    @JvmName("filtrerUgyldigeInternVedtak")
    fun filtrerUgyldigeVedtak(vedtak: List<InternVedtak>): List<InternVedtak> =
        filtrerUgyldige(
            vedtak = vedtak,
            id = { it.vedtakId },
            relatertVedtak = { it.relatertVedtak },
            fraOgMedDato = { it.periode.fraOgMedDato },
            tilOgMedDato = { it.periode.tilOgMedDato },
        )

    @JvmName("filtrerUgyldigeInternVedtakUtenUtbetaling")
    fun filtrerUgyldigeVedtak(
        vedtak: List<InternVedtakUtenUtbetaling>
    ): List<InternVedtakUtenUtbetaling> =
        filtrerUgyldige(
            vedtak = vedtak,
            id = { it.vedtakId },
            relatertVedtak = { it.relatertVedtak },
            fraOgMedDato = { it.periode.fraOgMedDato },
            tilOgMedDato = { it.periode.tilOgMedDato },
        )

    private fun <T> filtrerUgyldige(
        vedtak: List<T>,
        id: (T) -> String,
        relatertVedtak: (T) -> Any?,
        fraOgMedDato: (T) -> LocalDate?,
        tilOgMedDato: (T) -> LocalDate?,
    ): List<T> {
        val ugyldigeVedtak = vedtak.filter {
            val fom = fraOgMedDato(it)
            val tom = tilOgMedDato(it)
            fom != null && tom != null && fom > tom
        }

        val ugyldigeVedtakId = ugyldigeVedtak.map { id(it) }.toSet()

        return vedtak
            .filter {
                val fom = fraOgMedDato(it)
                val tom = tilOgMedDato(it)
                (fom == null || tom == null || fom <= tom)
                        && (relatertVedtak(it).toString() !in ugyldigeVedtakId)
            }
    }
}