package no.nav.aap.api.postgres

import no.nav.aap.api.kelvin.TilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilkjentYtelseTest {
    @Test
    fun `gradering etter uføre`() {
        val tilkjent = TilkjentYtelse(
            dagsats = 1000,
            gradering = 50,
            grunnlagsfaktor = 2.4.toBigDecimal(),
            grunnbeløp = 123321.toBigDecimal(),
            antallBarn = 0,
            barnetilleggsats = 37.toBigDecimal(),
            barnetillegg = 0.toBigDecimal(),
            samordningUføregradering = 30
        )

        val dagsatsEtterUføreReduksjon = tilkjent.regnUtDagsatsEtterUføreReduksjon()

        assertThat(dagsatsEtterUføreReduksjon).isEqualTo(700)
    }

}