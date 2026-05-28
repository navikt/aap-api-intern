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
    val virkningsperiode: PeriodeNullableTomDTO,
    /** Utfall settes relativt til [vedtaksType].
     * Er [vedtaksType] S (stans), så er utfall JA hvis det ble stans, og NEI hvis det ikke ble stans.
     * Mens er [vedtaksType] O (førstegangsbehandling), så er utfall JA hvis det er innvilgelse, og NEI hvis det er avslag.
     * Utfall er altså IKKE om det er et positivt vedtak for bruker eller om bruker får rett.
     * */
    val utfall: Utfall,
    val aktivitetsfase: DsopRettighetsTypeDTO,
    val vedtaksType: DsopVedtaksTypeDTO,
    val vedtaksvariant: DsopVedtaksvariantDTO?,
) {
    /** Dette er ikke Kelvins rettighetstype, men Arena sin rettighetstype som skiller
     * mellom AAP og andre ytelser.
     */
    val rettighetsType: String = "AAP"
}

public enum class Utfall { JA, NEI }

public enum class DsopVedtaksTypeDTO(description: String) {
    O("NY RETTIGHET"),
    E("ENDRING I RETTIGHET"),
    G("GJENINNTREDEN"),
    S("STANS ELLER OPPHØR"),
}

public enum class DsopVedtaksvariantDTO {
    O_AVSLAG,
    O_INNV_NAV,
    O_INNV_SOKNAD,
    E_FORLENGE,
    E_VERDI,
    G_AVSLAG,
    G_INNV_NAV,
    G_INNV_SOKNAD,
    S_DOD,
    S_OPPHOR,
    S_STANS,
    ;
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
