package api.postgres

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


data class DatadelingDTO(
    val underveisperiode: List<UnderveisDTO>,
    val rettighetsPeriodeFom: LocalDate,
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
    val beregningsgrunnlag: BigDecimal
)


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
}