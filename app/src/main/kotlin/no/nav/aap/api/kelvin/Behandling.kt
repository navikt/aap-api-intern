package no.nav.aap.api.kelvin

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

data class Behandling(
    val behandlingsReferanse: String,
    @Deprecated("Ikke del denne utad.")
    val rettighetsperiode: Periode,
    val underveisperiode: List<UnderveisIntern>,
    val behandlingStatus: KelvinBehandlingStatus,
    val vedtaksDato: LocalDate,
    val sak: Sak,
    val tilkjent: Tidslinje<TilkjentYtelse>,
    val rettighetsTypePerioder: List<RettighetsTypePeriode>,
    val samId: String?,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal?,
    val nyttVedtak: Boolean,
    val stansOpphørVurdering: Set<GjeldendeStansEllerOpphør>?,
    val arenakompatibleVedtak: List<Arenavedtak>,
) {
    val rettighetsTypeTidslinje: Tidslinje<String>
        get() = rettighetsTypePerioder.somTidslinje({ it.periode }, { it.verdi })
            .komprimer()
}

data class GjeldendeStansEllerOpphør(
    val fom: LocalDate,
    val opprettet: Instant,
    val vurdering: StansEllerOpphør,
    val avslagsårsaker: Set<Avslagsårsak>,
)

enum class Avslagsårsak(
    val type: StansEllerOpphør,
) {
    BRUKER_OVER_67(StansEllerOpphør.OPPHØR),
    IKKE_RETT_PA_SYKEPENGEERSTATNING(StansEllerOpphør.OPPHØR),
    IKKE_RETT_PA_STUDENT(StansEllerOpphør.OPPHØR),
    VARIGHET_OVERSKREDET_STUDENT(StansEllerOpphør.OPPHØR),
    IKKE_SYKDOM_AV_VISS_VARIGHET(StansEllerOpphør.OPPHØR),
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL(StansEllerOpphør.OPPHØR),
    IKKE_NOK_REDUSERT_ARBEIDSEVNE(StansEllerOpphør.OPPHØR),
    IKKE_BEHOV_FOR_OPPFOLGING(StansEllerOpphør.OPPHØR),
    IKKE_MEDLEM(StansEllerOpphør.OPPHØR),
    IKKE_OPPFYLT_OPPHOLDSKRAV_EØS(StansEllerOpphør.STANS),
    ANNEN_FULL_YTELSE(StansEllerOpphør.OPPHØR),
    INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING(StansEllerOpphør.OPPHØR),
    IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE(StansEllerOpphør.OPPHØR),
    VARIGHET_OVERSKREDET_OVERGANG_UFORE(StansEllerOpphør.OPPHØR),
    VARIGHET_OVERSKREDET_ARBEIDSSØKER(StansEllerOpphør.OPPHØR),
    IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER(StansEllerOpphør.STANS),
    IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING(StansEllerOpphør.STANS),
    BRUDD_PÅ_AKTIVITETSPLIKT_STANS(StansEllerOpphør.STANS),
    BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR(StansEllerOpphør.OPPHØR),
    BRUDD_PÅ_OPPHOLDSKRAV_STANS(StansEllerOpphør.STANS),
    BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR(StansEllerOpphør.OPPHØR),
    ORDINÆRKVOTE_BRUKT_OPP(StansEllerOpphør.OPPHØR),
    SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP(StansEllerOpphør.OPPHØR),
}

enum class StansEllerOpphør {
    STANS,
    OPPHØR
}

data class RettighetsTypePeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val verdi: String
) {
    val periode: Periode
        get() = Periode(fom, tom)
}

data class Sak(
    val saksnummer: String,
    val status: KelvinSakStatus,
    val opprettetTidspunkt: LocalDateTime,
)

/**
 * @param samordningUføregradering Svarer til prosent uføre.
 */
data class TilkjentYtelse(
    val dagsats: Int,
    val gradering: Int,
    val samordningUføregradering: Int?,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
) {
    fun gradertBarnetillegg(): BigDecimal =
        this.barnetillegg.multiply(
            this.gradering.toBigDecimal()
                .divide(100.toBigDecimal())
        ).setScale(0, RoundingMode.HALF_UP)

    fun regnUtDagsatsEtterUføreReduksjon(): Int =
        this.dagsats.times(
            (100 - (this.samordningUføregradering ?: 0)) / 100.0
        ).roundToInt()
}

data class UnderveisIntern(
    val underveisFom: LocalDate,
    val underveisTom: LocalDate,
    val meldeperiodeFom: LocalDate,
    val meldeperiodeTom: LocalDate,
    val utfall: String,
    val rettighetsType: String?,
    val avslagsårsak: String?, // skal ikke denne være Avslagsårsak?
)

enum class KelvinSakStatus {
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET
}


enum class KelvinBehandlingStatus {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    fun iverksatt() =
        this == IVERKSETTES || this == AVSLUTTET
}

data class Arenavedtak(
    val vedtakId: Long,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksvariant: Vedtaksvariant,
) {
    enum class Vedtaksvariant {
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
}