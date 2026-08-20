package no.nav.aap.api.arena

import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaVedtakFilterTest {

    @Test
    fun `filtrerer bort med fradato etter tildato`() {

        val fraOgMedDato = LocalDate.of(2026, 8, 20)
        val tilOgMedDato = LocalDate.of(2026, 8, 19)
        val vedtak = listOf(
            konstruerVedtak(fraOgMedDato, tilOgMedDato)
        )

        assertThat(ArenaVedtakFilter.filtrerUgyldigeVedtak(vedtak)).isEmpty()
    }

    @Test
    fun `stanser et endringsvedtak, stansen skal ikke med`() {
        val fraDato = LocalDate.of(2026, 8, 20)


        val vedtak = listOf(
            konstruerVedtak(fraDato, fraDato.plusYears(1))
                .copy(vedtaksId = "1", vedtaksTypeKode = "O", relatertVedtak = null),
            konstruerVedtak(fraDato.plusMonths(1), fraDato.plusMonths(1).minusDays(1))
                .copy(vedtaksId = "2", relatertVedtak = 1, vedtaksTypeKode = "E"),
            konstruerVedtak(fraDato.plusMonths(1), null)
                .copy(vedtaksId = "3", relatertVedtak = 2, vedtaksTypeKode = "S"),
            konstruerVedtak(fraDato.plusMonths(2), null)
                .copy(vedtaksId = "4", relatertVedtak = 1, vedtaksTypeKode = "E"),
        )

        assertThat(
            ArenaVedtakFilter.filtrerUgyldigeVedtak(vedtak)
                .map { it.vedtaksId }).containsExactly("1", "4")
    }

    private fun konstruerVedtak(
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate?
    ): Vedtak = Vedtak(
        vedtaksId = "mea",
        utbetaling = listOf(),
        dagsats = 3277,
        status = "AVSLU",
        utfallkode = "JA",
        saksnummer = "atqui",
        vedtaksdato = "nec",
        vedtaksTypeKode = "graeco",
        vedtaksTypeNavn = "reprimique",
        periode = Periode(
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato
        ),
        rettighetsType = "AAP",
        beregningsgrunnlag = 7792,
        barnMedStonad = 7121,
        barnetillegg = 8465,
        barnetilleggsats = 2966,
        justertG = null,
        lopenrvedtak = 2839,
        relatertVedtak = 2639
    )
}