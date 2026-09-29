package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import java.math.BigDecimal
import java.time.LocalDate

public data class BisysBarnetilleggRequest(
    val personidentifikator: String,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}

public data class BisysBarnetilleggResponse(
    @property:Description("Liste med barnetillegg per barn. Hvis identen til barnet ikke er kjent, er denne null.")
    val barnMedBarnetillegg: List<BisysBarnMedBarnetillegg>,
)

public data class BisysBarnMedBarnetillegg(
    val ident: String?,
    val perioderMedBarnetillegg: List<BisysPeriodeMedBeløp>,
)

public data class BisysPeriodeMedBeløp(
    val fra: LocalDate,
    val til: LocalDate,
    @property:Description("Tilkjent barnetillegg etter ev redusering (samordning og/eller arbeid).")
    val beløp: BigDecimal,
    @property:Description("Barnetilleggsats per barn.")
    val sats: BigDecimal,
    @property:Description("Barnetillegg før reduksjoner.")
    val uredusertBeløp: BigDecimal
)
