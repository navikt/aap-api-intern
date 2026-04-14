package no.nav.aap.api.postgres

import no.nav.aap.behandlingsflyt.kontrakt.datadeling.StansEllerOpphørEnumDTO
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingData(
    val behandlingsId: String,
    val behandlingsReferanse: String,
    val underveisperiode: List<UnderveisDTO>,
    val behandlingStatus: KelvinBehandlingStatus,
    val vedtaksDato: LocalDate,
    val sak: SakInfo,
    val tilkjent: List<TilkjentDTO>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val samId: String?,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal?,
    val nyttVedtak: Boolean,
    val stansOpphørVurdering: Set<GjeldendeStansEllerOpphørDTO>?,
)

data class SakInfo(
    val saksnummer: String,
    val status: KelvinSakStatus,
)

fun DatadelingDTO.tilBehandlingData(): BehandlingData = BehandlingData(
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

data class DatadelingDTO(
    val underveisperiode: List<UnderveisDTO>,
    @Deprecated("Ikke del disse utad.")
    val rettighetsPeriodeFom: LocalDate,
    @Deprecated("Ikke del disse utad.")
    val rettighetsPeriodeTom: LocalDate,
    val behandlingStatus: KelvinBehandlingStatus,
    val behandlingsId: String,
    val vedtaksDato: LocalDate,
    val sak: SakDTO,
    val tilkjent: List<TilkjentDTO>,
    val rettighetsTypeTidsLinje: List<RettighetsTypePeriode>,
    val behandlingsReferanse: String,
    val samId: String? = null,
    val vedtakId: Long,
    val beregningsgrunnlag: BigDecimal?,
    val nyttVedtak: Boolean,
    val stansOpphørVurdering: Set<GjeldendeStansEllerOpphørDTO>?
)

data class GjeldendeStansEllerOpphørDTO(
    val fom: LocalDate,
    val opprettet: Instant,
    val vurdering: StansEllerOpphørEnumDTODomene,
    val avslagsårsaker: Set<AvslagsårsakDTO>,
)

public enum class AvslagsårsakDTO(
    public val type: StansEllerOpphørEnumDTO,
) {
    BRUKER_OVER_67(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_SYKEPENGEERSTATNING(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_STUDENT(StansEllerOpphørEnumDTO.OPPHØR),
    VARIGHET_OVERSKREDET_STUDENT(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_SYKDOM_AV_VISS_VARIGHET(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_NOK_REDUSERT_ARBEIDSEVNE(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_BEHOV_FOR_OPPFOLGING(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_MEDLEM(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_OPPFYLT_OPPHOLDSKRAV_EØS(StansEllerOpphørEnumDTO.STANS),
    ANNEN_FULL_YTELSE(StansEllerOpphørEnumDTO.OPPHØR),
    INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE(StansEllerOpphørEnumDTO.OPPHØR),
    VARIGHET_OVERSKREDET_OVERGANG_UFORE(StansEllerOpphørEnumDTO.OPPHØR),
    VARIGHET_OVERSKREDET_ARBEIDSSØKER(StansEllerOpphørEnumDTO.OPPHØR),
    IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER(StansEllerOpphørEnumDTO.STANS),
    IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING(StansEllerOpphørEnumDTO.STANS),
    BRUDD_PÅ_AKTIVITETSPLIKT_STANS(StansEllerOpphørEnumDTO.STANS),
    BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR(StansEllerOpphørEnumDTO.OPPHØR),
    BRUDD_PÅ_OPPHOLDSKRAV_STANS(StansEllerOpphørEnumDTO.STANS),
    BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR(StansEllerOpphørEnumDTO.OPPHØR),
    ORDINÆRKVOTE_BRUKT_OPP(StansEllerOpphørEnumDTO.OPPHØR),
    SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP(StansEllerOpphørEnumDTO.OPPHØR),
}

enum class StansEllerOpphørEnumDTODomene {
    STANS,
    OPPHØR
}

data class RettighetsTypePeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val verdi: String
)

data class SakDTO(
    val saksnummer: String,
    val status: KelvinSakStatus,
    val fnr: List<String>,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

/**
 * @param samordningUføregradering Svarer til prosent uføre. 100% bør medføre 0% gradering.
 */
data class TilkjentDTO(
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

data class UnderveisDTO(
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