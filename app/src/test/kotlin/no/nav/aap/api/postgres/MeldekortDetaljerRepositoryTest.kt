package no.nav.aap.api.postgres

import no.nav.aap.api.kelvin.MeldekortDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortDetaljerRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `lagre og hente ut`() {
        val meldekortDTO = MeldekortDTO(
            personIdent = "12345678901",
            saksnummer = "asd123",
            mottattTidspunkt = LocalDateTime.now(),
            behandlingId = 1234,
            meldepliktStatusKode = null,
            rettighetsTypeKode = null,
            meldePeriode = Periode(
                LocalDate.of(2025, 4, 14),
                LocalDate.of(2025, 4, 23)
            ),
            arbeidPerDag = listOf(
                MeldekortDTO.MeldeDag(
                    dag = LocalDate.of(2025, 4, 15),
                    timerArbeidet = 7.toBigDecimal()
                ),
                MeldekortDTO.MeldeDag(
                    dag = LocalDate.of(2025, 4, 23),
                    timerArbeidet = 3.toBigDecimal()
                )
            ),
            avslagsårsakKode = null,
        )
        dataSource.transaction {
            MeldekortDetaljerRepository(it).lagre(
                listOf(meldekortDTO)
            )
        }

        val res = dataSource.transaction {
            MeldekortDetaljerRepository(it).hentAlle(
                personIdentifikatorer = listOf("12345678901"),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 12, 31)
            )
        }

        assertThat(res)
            .usingRecursiveComparison()
            .ignoringFields("mottattTidspunkt")
            .withComparatorForType({ a, b -> a.toDouble().compareTo(b.toDouble()) },
                BigDecimal::class.java)
            .isEqualTo(listOf(meldekortDTO))
    }

    @Test
    fun `slå sammen meldekort til ett`() {
        val periode = Periode(LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 16))
        val meldekort = listOf(
            Meldekort(
                periode = periode,
                antallTimerArbeidet = BigDecimal.valueOf(10),
                timerArbeidetPerDag = listOf(
                    TimerArbeidetPerDag(LocalDate.of(2025, 4, 14), 5.0),
                    TimerArbeidetPerDag(LocalDate.of(2025, 4, 15), 5.0)
                ),
                sistOppdatert = LocalDateTime.of(2025, 4, 17, 10, 0)
            ),
            Meldekort(
                periode = periode,
                antallTimerArbeidet = BigDecimal.valueOf(5),
                timerArbeidetPerDag = listOf(
                    TimerArbeidetPerDag(LocalDate.of(2025, 4, 15), 2.0),
                    TimerArbeidetPerDag(LocalDate.of(2025, 4, 16), 3.0)
                ),
                sistOppdatert = LocalDateTime.of(2025, 4, 18, 10, 0)
            )
        )

        val res = meldekort.slåSammenMeldeperioder()

        assertThat(res).hasSize(1)
        val sammenslått = res.first()
        assertThat(sammenslått.periode).isEqualTo(periode)

        // Meldekort nr 2 er korrigert. Så den 15de er det 2 timer arbeidet. 5+2+3=10
        assertThat(sammenslått.timerArbeidetPerDag.sumOf { it.timerArbeidet }).isEqualByComparingTo(10.0)
        assertThat(sammenslått.timerArbeidetPerDag).containsExactlyInAnyOrder(
            TimerArbeidetPerDag(LocalDate.of(2025, 4, 14), 5.0),
            TimerArbeidetPerDag(LocalDate.of(2025, 4, 15), 2.0),
            TimerArbeidetPerDag(LocalDate.of(2025, 4, 16), 3.0)
        )
        assertThat(sammenslått.sistOppdatert).isEqualTo(LocalDateTime.of(2025, 4, 18, 10, 0))
    }

    @Test
    fun `slå sammen meldekort med ulike perioder`() {
        val periode1 = Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 5))
        val periode2 = Periode(LocalDate.of(2025, 4, 6), LocalDate.of(2025, 4, 10))

        val meldekort = listOf(
            Meldekort(
                periode = periode1,
                antallTimerArbeidet = BigDecimal.valueOf(5),
                timerArbeidetPerDag = listOf(TimerArbeidetPerDag(LocalDate.of(2025, 4, 1), 5.0)),
                sistOppdatert = LocalDateTime.now()
            ),
            Meldekort(
                periode = periode2,
                antallTimerArbeidet = BigDecimal.valueOf(10),
                timerArbeidetPerDag = listOf(TimerArbeidetPerDag(LocalDate.of(2025, 4, 6), 10.0)),
                sistOppdatert = LocalDateTime.now()
            )
        )

        val res = meldekort.slåSammenMeldeperioder()

        assertThat(res).hasSize(2)
        assertThat(res.map { it.periode }).containsExactlyInAnyOrder(periode1, periode2)
    }

}