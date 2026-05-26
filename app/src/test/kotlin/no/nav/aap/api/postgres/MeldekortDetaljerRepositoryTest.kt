package no.nav.aap.api.postgres

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.api.kelvin.Meldekort
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        val meldekort = Meldekort(
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
                Meldekort.MeldeDag(
                    dag = LocalDate.of(2025, 4, 15),
                    timerArbeidet = 7.toBigDecimal()
                ),
                Meldekort.MeldeDag(
                    dag = LocalDate.of(2025, 4, 23),
                    timerArbeidet = 3.toBigDecimal()
                )
            ),
        )
        dataSource.transaction {
            MeldekortDetaljerRepository(it).lagre(
                listOf(meldekort)
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
            .isEqualTo(listOf(meldekort))
    }

    @Test
    fun `lagre med nyere behandlingId erstatter eldre meldekort for samme sak`() {
        val gammeltMeldekort = Meldekort(
            personIdent = "12345678901",
            saksnummer = "sak-1",
            mottattTidspunkt = LocalDateTime.now().minusDays(1),
            behandlingId = 10,
            meldepliktStatusKode = null,
            rettighetsTypeKode = null,
            meldePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14)),
            arbeidPerDag = listOf(
                Meldekort.MeldeDag(LocalDate.of(2025, 1, 2), 1.toBigDecimal())
            ),
        )

        val nyttMeldekort = gammeltMeldekort.copy(
            mottattTidspunkt = LocalDateTime.now(),
            behandlingId = 11,
            meldePeriode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 28)),
            arbeidPerDag = listOf(
                Meldekort.MeldeDag(LocalDate.of(2025, 1, 16), 2.toBigDecimal())
            )
        )

        dataSource.transaction {
            val repository = MeldekortDetaljerRepository(it)
            repository.lagre(listOf(gammeltMeldekort))
            repository.lagre(listOf(nyttMeldekort))
        }

        val resultater = dataSource.transaction {
            MeldekortDetaljerRepository(it).hentAlle(
                personIdentifikatorer = listOf("12345678901"),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 12, 31)
            )
        }

        assertThat(resultater).hasSize(1)
        assertThat(resultater.single().behandlingId).isEqualTo(11)
        assertThat(resultater.single().meldePeriode.fom).isEqualTo(LocalDate.of(2025, 1, 15))
    }
}