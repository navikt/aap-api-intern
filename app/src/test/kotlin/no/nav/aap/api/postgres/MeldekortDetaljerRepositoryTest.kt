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

}