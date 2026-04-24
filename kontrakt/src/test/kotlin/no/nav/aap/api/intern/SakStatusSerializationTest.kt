package no.nav.aap.api.intern

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakStatusSerializationTest {

    @Test
    fun `serialiserer og deserialiserer Arena SakStatus`() {
        val original = SakStatus.Arena(
            statusKode = ArenaStatus.IVERK,
            periode = Periode(
                fraOgMedDato = LocalDate.of(2024, 1, 1),
                tilOgMedDato = LocalDate.of(2024, 12, 31)
            ),
            sakId = "ARENA123"
        )

        val json = DefaultJsonMapper.toJson(original)
        val deserialisert = DefaultJsonMapper.fromJson<SakStatus>(json)

        assertThat(deserialisert)
            .isInstanceOf(SakStatus.Arena::class.java)
            .usingRecursiveComparison()
            .isEqualTo(original)
    }

    @Test
    fun `serialiserer og deserialiserer Kelvin SakStatus`() {
        val original = SakStatus.Kelvin(
            statusKode = KelvinStatus.LØPENDE,
            periode = Periode(
                fraOgMedDato = LocalDate.of(2024, 1, 1),
                tilOgMedDato = LocalDate.of(2024, 12, 31)
            ),
            perioder = listOf(
                Periode(
                    fraOgMedDato = LocalDate.of(2024, 1, 1),
                    tilOgMedDato = LocalDate.of(2024, 6, 30)
                )
            ),
            sakId = "KELVIN456",
            enhet = NåværendeEnhet(
                oversendtDato = LocalDate.of(2024, 3, 1),
                oppgaveKategori = OppgaveKategori.NAY,
                enhet = "4291"
            )
        )

        val json = DefaultJsonMapper.toJson(original)
        val deserialisert = DefaultJsonMapper.fromJson<SakStatus>(json)

        assertThat(deserialisert)
            .isInstanceOf(SakStatus.Kelvin::class.java)
            .usingRecursiveComparison()
            .isEqualTo(original)
    }
}
