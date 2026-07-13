package no.nav.aap.api.maksimum

import java.time.LocalDate
import no.nav.aap.api.intern.Kilde as KontraktKilde
import no.nav.aap.api.intern.Maksimum as KontraktMaksimum
import no.nav.aap.api.intern.Medium as KontraktMedium
import no.nav.aap.api.intern.Periode as KontraktPeriode
import no.nav.aap.api.intern.Reduksjon as KontraktReduksjon
import no.nav.aap.api.intern.UtbetalingMedMer as KontraktUtbetalingMedMer
import no.nav.aap.api.intern.Vedtak as KontraktVedtak
import no.nav.aap.api.intern.VedtakUtenUtbetaling as KontraktVedtakUtenUtbetaling
import no.nav.aap.api.util.fraKontrakt
import no.nav.aap.api.util.fraKontraktUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum as ArenaMaksimum
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as ArenaPeriode
import no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer as ArenaUtbetalingMedMer
import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak as ArenaVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InternMaksimumTest {
    @Test
    fun `fraKontrakt bevarer interne Arena-felt og tilKontrakt skjuler dem`() {
        val arenaMaksimum = ArenaMaksimum(
            vedtak = listOf(
                ArenaVedtak(
                    vedtaksId = "vedtak-1",
                    utbetaling = listOf(
                        ArenaUtbetalingMedMer(
                            reduksjon = no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon(
                                timerArbeidet = 1.5,
                                annenReduksjon = no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon(
                                    sykedager = 0.5F,
                                    sentMeldekort = true,
                                    fraver = 2.0F,
                                ),
                            ),
                            periode = ArenaPeriode(
                                fraOgMedDato = LocalDate.of(2024, 1, 1),
                                tilOgMedDato = LocalDate.of(2024, 1, 31),
                            ),
                            belop = 1000,
                            dagsats = 100,
                            barnetillegg = 50,
                        )
                    ),
                    dagsats = 100,
                    status = "LØPENDE",
                    saksnummer = "sak-1",
                    vedtaksdato = "2024-01-01",
                    vedtaksTypeKode = "E",
                    vedtaksTypeNavn = "Endring",
                    periode = ArenaPeriode(
                        fraOgMedDato = LocalDate.of(2024, 1, 1),
                        tilOgMedDato = LocalDate.of(2024, 1, 31),
                    ),
                    rettighetsType = "AAP",
                    beregningsgrunnlag = 123,
                    barnMedStonad = 1,
                    barnetillegg = 50,
                    barnetilleggsats = 0,
                    justertG = null,
                    lopenrvedtak = 42,
                    relatertVedtak = 41,
                )
            )
        )

        val internMaksimum = arenaMaksimum.fraKontrakt()

        assertThat(internMaksimum).usingRecursiveComparison().isEqualTo(
            InternMaksimum(
                vedtak = listOf(
                    InternVedtak(
                        dagsats = 100,
                        dagsatsEtterUføreReduksjon = null,
                        vedtakId = "vedtak-1",
                        status = "LØPENDE",
                        saksnummer = "sak-1",
                        vedtaksdato = LocalDate.of(2024, 1, 1),
                        periode = InternPeriode(
                            fraOgMedDato = LocalDate.of(2024, 1, 1),
                            tilOgMedDato = LocalDate.of(2024, 1, 31),
                        ),
                        rettighetsType = "AAP",
                        beregningsgrunnlag = 123,
                        barnMedStonad = 1,
                        barnetillegg = 50,
                        barnetilleggSats = 0,
                        kildesystem = InternKilde.ARENA,
                        samordningsId = null,
                        opphorsAarsak = null,
                        vedtaksTypeKode = "E",
                        vedtaksTypeNavn = "Endring",
                        utbetaling = listOf(
                            InternUtbetalingMedMer(
                                reduksjon = InternReduksjon(
                                    timerArbeidet = 1.5,
                                    annenReduksjon = InternAnnenReduksjon(
                                        sykedager = 0.5F,
                                        sentMeldekort = 1,
                                        fravær = 2.0F,
                                    ),
                                ),
                                utbetalingsgrad = null,
                                periode = InternPeriode(
                                    fraOgMedDato = LocalDate.of(2024, 1, 1),
                                    tilOgMedDato = LocalDate.of(2024, 1, 31),
                                ),
                                belop = 1000,
                                dagsats = 100,
                                barnetillegg = 50,
                            )
                        ),
                        lopenrvedtak = 42,
                        relatertVedtak = 41,
                    )
                )
            )
        )

        assertThat(internMaksimum.tilKontrakt()).usingRecursiveComparison().isEqualTo(
            KontraktMaksimum(
                vedtak = listOf(
                    KontraktVedtak(
                        dagsats = 100,
                        dagsatsEtterUføreReduksjon = null,
                        vedtakId = "vedtak-1",
                        status = "LØPENDE",
                        saksnummer = "sak-1",
                        vedtaksdato = LocalDate.of(2024, 1, 1),
                        periode = KontraktPeriode(
                            fraOgMedDato = LocalDate.of(2024, 1, 1),
                            tilOgMedDato = LocalDate.of(2024, 1, 31),
                        ),
                        rettighetsType = "AAP",
                        beregningsgrunnlag = 123,
                        barnMedStonad = 1,
                        barnetillegg = 50,
                        barnetilleggSats = 0,
                        kildesystem = KontraktKilde.ARENA,
                        samordningsId = null,
                        opphorsAarsak = null,
                        vedtaksTypeKode = "E",
                        vedtaksTypeNavn = "Endring",
                        utbetaling = listOf(
                            KontraktUtbetalingMedMer(
                                reduksjon = KontraktReduksjon(
                                    timerArbeidet = 1.5,
                                    annenReduksjon = 3.5F,
                                ),
                                utbetalingsgrad = null,
                                periode = KontraktPeriode(
                                    fraOgMedDato = LocalDate.of(2024, 1, 1),
                                    tilOgMedDato = LocalDate.of(2024, 1, 31),
                                ),
                                belop = 1000,
                                dagsats = 100,
                                barnetillegg = 50,
                            )
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun `fraKontraktUtenUtbetaling bevarer interne Arena-felt og tilKontrakt skjuler dem`() {
        val arenaVedtak = ArenaVedtak(
            vedtaksId = "vedtak-2",
            utbetaling = listOf(
                ArenaUtbetalingMedMer(
                    periode = ArenaPeriode(
                        fraOgMedDato = LocalDate.of(2024, 2, 1),
                        tilOgMedDato = LocalDate.of(2024, 2, 29),
                    ),
                    belop = 2000,
                    dagsats = 200,
                    barnetillegg = 60,
                )
            ),
            dagsats = 200,
            status = "LØPENDE",
            saksnummer = "sak-2",
            vedtaksdato = "2024-02-01",
            vedtaksTypeKode = "O",
            vedtaksTypeNavn = "Original",
            periode = ArenaPeriode(
                fraOgMedDato = LocalDate.of(2024, 2, 1),
                tilOgMedDato = LocalDate.of(2024, 2, 29),
            ),
            rettighetsType = "AAP",
            beregningsgrunnlag = 456,
            barnMedStonad = 2,
            barnetillegg = 60,
            barnetilleggsats = 0,
            justertG = null,
            lopenrvedtak = 52,
            relatertVedtak = 51,
        )

        val internVedtak = arenaVedtak.fraKontraktUtenUtbetaling()

        assertThat(internVedtak).usingRecursiveComparison().isEqualTo(
            InternVedtakUtenUtbetaling(
                dagsats = 200,
                dagsatsEtterUføreReduksjon = null,
                vedtakId = "vedtak-2",
                status = "LØPENDE",
                saksnummer = "sak-2",
                vedtaksdato = LocalDate.of(2024, 2, 1),
                vedtaksTypeKode = "O",
                vedtaksTypeNavn = "Original",
                periode = InternPeriode(
                    fraOgMedDato = LocalDate.of(2024, 2, 1),
                    tilOgMedDato = LocalDate.of(2024, 2, 29),
                ),
                rettighetsType = "AAP",
                beregningsgrunnlag = 456,
                barnMedStonad = 2,
                barnetillegg = 60,
                kildesystem = InternKilde.ARENA,
                samordningsId = null,
                opphorsAarsak = null,
                lopenrvedtak = 52,
                relatertVedtak = 51,
            )
        )

        assertThat(InternMedium(vedtak = listOf(internVedtak)).tilKontrakt())
            .usingRecursiveComparison()
            .isEqualTo(
                KontraktMedium(
                    vedtak = listOf(
                        KontraktVedtakUtenUtbetaling(
                            dagsats = 200,
                            dagsatsEtterUføreReduksjon = null,
                            vedtakId = "vedtak-2",
                            status = "LØPENDE",
                            saksnummer = "sak-2",
                            vedtaksdato = LocalDate.of(2024, 2, 1),
                            vedtaksTypeKode = "O",
                            vedtaksTypeNavn = "Original",
                            periode = KontraktPeriode(
                                fraOgMedDato = LocalDate.of(2024, 2, 1),
                                tilOgMedDato = LocalDate.of(2024, 2, 29),
                            ),
                            rettighetsType = "AAP",
                            beregningsgrunnlag = 456,
                            barnMedStonad = 2,
                            barnetillegg = 60,
                            kildesystem = KontraktKilde.ARENA,
                            samordningsId = null,
                            opphorsAarsak = null,
                        )
                    )
                )
            )
    }
}
