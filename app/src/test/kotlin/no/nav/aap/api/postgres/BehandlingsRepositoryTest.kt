package no.nav.aap.api.postgres

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
import java.util.UUID

class BehandlingsRepositoryTest {
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
        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                DatadelingDTO(
                    underveisperiode = listOf(),
                    rettighetsPeriodeFom = LocalDate.of(2021, 1, 1),
                    rettighetsPeriodeTom = LocalDate.of(2022, 1, 1),
                    behandlingStatus = KelvinBehandlingStatus.UTREDES,
                    behandlingsId = "123",
                    vedtaksDato = LocalDate.now(),
                    sak = SakDTO(
                        saksnummer = "ABCDE",
                        status = KelvinSakStatus.OPPRETTET,
                        fnr = listOf("123445"),
                        opprettetTidspunkt = LocalDateTime.now()
                    ),
                    tilkjent = listOf(),
                    rettighetsTypeTidsLinje = listOf(),
                    behandlingsReferanse = UUID.randomUUID().toString(),
                    samId = null,
                    vedtakId = 1234L,
                    beregningsgrunnlag = BigDecimal.ZERO,
                    nyttVedtak = false
                )
            )
        }

        val uthentetVedtak = dataSource.transaction {
            BehandlingsRepository(it).hentVedtaksData(
                "123445",
                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
            )
        }

        assertThat(uthentetVedtak).hasSize(1)
    }
}