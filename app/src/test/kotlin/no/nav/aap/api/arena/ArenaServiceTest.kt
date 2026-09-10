package no.nav.aap.api.arena

import kotlinx.coroutines.runBlocking
import no.nav.aap.api.util.FakeArenaGateway
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode
import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaServiceTest {

    @Test
    fun `skal sortere vedtak på til-dato, deretter fra dato hvis null`(): Unit = runBlocking {
        // Realistisk vedtakshistorikk: sekvensielle, ikke-overlappende perioder,
        // der det siste vedtaket løper videre uten til-dato.
        // Sorteringsnøkkel er til-dato, eller fra-dato hvis til-dato mangler:
        //
        //   jan     feb     mar     apr     mai     jun
        //    |---A---|                                    A: 01.01-28.02 -> nøkkel 28.02
        //            |---B---|                            B: 01.03-30.04 -> nøkkel 30.04
        //                    |---C--->                    C: 01.05-null  -> nøkkel 01.05 (løpende)
        //
        // Input kommer i vilkårlig rekkefølge (C, A, B), men skal sorteres til A, B, C.
        val arenaService = ArenaService(
            arena = FakeArenaGateway(maksimum = Maksimum(vedtak = listOf(vedtakC, vedtakA, vedtakB))),
            arenaHistorikk = FakeArenaGateway(),
            erProd = false,
        )

        val vedtak = arenaService.hentVedtak(
            callId = "test-call-id",
            vedtakRequest = InternVedtakRequest(personidentifikator = "12345678910"),
        )

        assertThat(vedtak.map { it.vedtakId }).containsExactly("A", "B", "C")
    }

    @Test
    fun `skal ikke sortere vedtak i prod`(): Unit = runBlocking {
        val arenaService = ArenaService(
            arena = FakeArenaGateway(maksimum = Maksimum(vedtak = listOf(vedtakC, vedtakA, vedtakB))),
            arenaHistorikk = FakeArenaGateway(),
            erProd = true,
        )

        val vedtak = arenaService.hentVedtak(
            callId = "test-call-id",
            vedtakRequest = InternVedtakRequest(personidentifikator = "12345678910"),
        )

        assertThat(vedtak.map { it.vedtakId }).containsExactly("C", "A", "B")
    }

    private val vedtakA = konstruerVedtak(
        vedtaksId = "A",
        fraOgMedDato = LocalDate.of(2024, 1, 1),
        tilOgMedDato = LocalDate.of(2024, 2, 28),
    )
    private val vedtakB = konstruerVedtak(
        vedtaksId = "B",
        fraOgMedDato = LocalDate.of(2024, 3, 1),
        tilOgMedDato = LocalDate.of(2024, 4, 30),
    )
    private val vedtakC = konstruerVedtak(
        vedtaksId = "C",
        fraOgMedDato = LocalDate.of(2024, 5, 1),
        tilOgMedDato = null,
    )

    private fun konstruerVedtak(
        vedtaksId: String,
        fraOgMedDato: LocalDate,
        tilOgMedDato: LocalDate?,
    ): Vedtak = Vedtak(
        vedtaksId = vedtaksId,
        utbetaling = listOf(),
        dagsats = 100,
        status = "IVERK",
        utfallkode = "JA",
        saksnummer = "sak-$vedtaksId",
        vedtaksdato = "2024-01-01",
        vedtaksTypeKode = "O",
        vedtaksTypeNavn = "Ordinær",
        periode = Periode(
            fraOgMedDato = fraOgMedDato,
            tilOgMedDato = tilOgMedDato,
        ),
        rettighetsType = "AAP",
        beregningsgrunnlag = 0,
        barnMedStonad = 0,
        barnetillegg = 0,
        justertG = null,
        lopenrvedtak = null,
        relatertVedtak = null,
    )

}