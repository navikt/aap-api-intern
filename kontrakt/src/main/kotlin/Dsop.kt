package no.nav.aap.api.intern

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

public data class DsopMeldekortRespons(
    val uttrekksperiode: PeriodeDTO,
    val meldekort: List<DsopMeldekortDTO>
)

public data class DsopMeldekortDTO(
    val periode: PeriodeDTO,
    @Deprecated("Bruk timerArbeidetPerDag.")
    val antallTimerArbeidet: BigDecimal,
    val timerArbeidetPerDag: List<DsopTimerArbeidetPerDagDTO>,
    val sistOppdatert: LocalDateTime,
)

public data class DsopTimerArbeidetPerDagDTO(
    val dag: LocalDate,
    val timerArbeidet: Double,
)

public data class DsopRequest(
    val personIdent: String,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
)

public data class DsopResponse(
    val uttrekksperiode: PeriodeDTO,
    val vedtak: List<DsopVedtakDTO>,
)

public data class DsopVedtakDTO(
    val vedtakId: String,
    val vedtakStatus: DsopStatusDTO,
    val virkningsperiode: PeriodeDTO,
    val rettighetsType: String = "AAP",
    val utfall: String = "JA",
    val aktivitetsfase: DsopRettighetsTypeDTO,
    val vedtaksType: DsopVedtaksTypeDTO,
)

public enum class DsopVedtaksTypeDTO(description: String) {
    O("NY RETTIGHET"),
    E("ENDRING I RETTIGHET")
}

public enum class DsopStatusDTO {
    LØPENDE,
    AVSLUTTET
}

public enum class DsopRettighetsTypeDTO {
    BISTANDSBEHOV,
    SYKEPENGEERSTATNING,
    STUDENT,
    ARBEIDSSØKER,
    VURDERES_FOR_UFØRETRYGD,
}
