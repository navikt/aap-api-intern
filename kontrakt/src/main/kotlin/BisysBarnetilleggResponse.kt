package no.nav.aap.api.intern

import java.math.BigDecimal
import java.time.LocalDate
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

public data class BisysBarnetilleggRequest(
    val personidentifikator: String,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}

public data class BisysBarnetilleggResponse(
    val barnMedBarnetillegg: List<BisysBarnMedBarnetillegg>,
)

public data class BisysBarnMedBarnetillegg(
    val ident: String?,
    val perioderMedBarnetillegg: List<BisysPeriodeMedBeløp>,
)

public data class BisysPeriodeMedBeløp(
    val fra: LocalDate,
    val til: LocalDate,
    val beløp: BigDecimal,
)
