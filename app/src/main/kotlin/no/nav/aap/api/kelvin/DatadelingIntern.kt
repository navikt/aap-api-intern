package no.nav.aap.api.kelvin

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingData(
    val behandlingsId: String,
    val behandlingsReferanse: String,
    val underveisperiode: List<UnderveisIntern>,
    val behandlingStatus: KelvinBehandlingStatus,
    val vedtaksDato: LocalDate,
    val sak: SakInfo,
    val tilkjent: List<TilkjentPeriode>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val samId: String?,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal?,
    val nyttVedtak: Boolean,
    val stansOpphørVurdering: Set<GjeldendeStansEllerOpphør>?,
)

data class SakInfo(
    val saksnummer: String,
    val status: KelvinSakStatus,
)

fun DatadelingIntern.tilBehandlingData(): BehandlingData = BehandlingData(
    behandlingsId = this.behandlingsId,
    behandlingsReferanse = this.behandlingsReferanse,
    underveisperiode = this.underveisperiode,
    behandlingStatus = this.behandlingStatus,
    vedtaksDato = this.vedtaksDato,
    sak = SakInfo(saksnummer = this.sak.saksnummer, status = this.sak.status),
    tilkjent = this.tilkjent,
    rettighetsTypeTidsLinje = this.rettighetsTypeTidsLinje,
    samId = this.samId,
    vedtakId = this.vedtakId,
    beregningsgrunnlag = this.beregningsgrunnlag,
    nyttVedtak = this.nyttVedtak,
    stansOpphørVurdering = this.stansOpphørVurdering,
)

data class DatadelingIntern(
    val underveisperiode: List<UnderveisIntern>,
    @Deprecated("Ikke del disse utad.")
    val rettighetsPeriodeFom: LocalDate,
    @Deprecated("Ikke del disse utad.")
    val rettighetsPeriodeTom: LocalDate,
    val behandlingStatus: KelvinBehandlingStatus,
    val behandlingsId: String,
    val vedtaksDato: LocalDate,
    val sak: Sak,
    val tilkjent: List<TilkjentPeriode>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val behandlingsReferanse: String,
    val samId: String? = null,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal?,
    val nyttVedtak: Boolean,
    val stansOpphørVurdering: Set<GjeldendeStansEllerOpphør>?
)

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
)

data class Sak(
    val saksnummer: String,
    val status: KelvinSakStatus,
    val fnr: List<String>,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

/**
 * @param samordningUføregradering Svarer til prosent uføre. 100% bør medføre 0% gradering.
 */
data class TilkjentPeriode(
    val tilkjentFom: LocalDate,
    val tilkjentTom: LocalDate,
    val dagsats: Int,
    val gradering: Int,
    val samordningUføregradering: Int? = null,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal
)

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