package no.nav.aap.api.postgres

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilkjentDBTest {
    @Test
    fun `gradering etter uføre`() {
        val tilkjent = TilkjentDB(
            dagsats = 1000,
            gradering = 50,
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 0,
            barnetilleggsats = 37.toBigDecimal(),
            barnetillegg = 0.toBigDecimal(),
            uføregrad = 30
        )

        val dagsatsEtterUføreReduksjon = tilkjent.regnUtDagsatsEtterUføreReduksjon()

        assertThat(dagsatsEtterUføreReduksjon).isEqualTo(700)
    }

}