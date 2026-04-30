package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.time.LocalDate

public data class Maksimum(
    @property:Description("""En liste med "vedtak". For Arena-saker svarer hvert element til et vedtak, mens for Kelvin-saker er listen brutt opp etter type AAP (rettighetstype).""")
    val vedtak: List<Vedtak>
)

public data class Medium(val vedtak: List<VedtakUtenUtbetaling>)

public data class InternVedtakRequestApiIntern(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate? = LocalDate.of(1, 1, 1),
    val tilOgMedDato: LocalDate? = LocalDate.of(9999, 12, 31)
)

/**
 * @param status Hypotese, vedtaksstatuskode
 * @param saksnummer hypotese sak_id
 * @param dagsats Dagsats før reduksjoner.
 */
public data class Vedtak(
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int?,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate,
    val periode: Periode,
    val rettighetsType: String,
    val beregningsgrunnlag: Int,

    /** Antall barn som gir rett til barnetillegg. */
    val barnMedStonad: Int,

    /** Størrelsen på total, ugradert barnetillegg.
     *
     * Verdien er total i den forstand at den tar hensyn til antall barn.
     *
     * Den er ugradert i den forstand at hvis medlemmet har 2 barn, får 75 % AAP
     * på grunn av samordning, og barnetilleggssatsen er spesifisert i AAP-forskriften § 8 til 38 kroner,
     * så vil [barnetillegg] være 2 * 38 = 76 kroner. Altså vi har ikke redusert barnetillegget med 25% her.
     *
     * Spesifikasjon: [barnetillegg] = [barnetilleggSats] * [barnMedStonad].
     */
    val barnetillegg: Int,

    /** Størrelsen på ugradert barnetilleggsats.
     *
     * Verdien er ugradert, i den forstand at:
     * Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
     * så vil [barnetilleggSats] være 38.
     **/
    val barnetilleggSats: Int,
    val kildesystem: Kilde,
    val samordningsId: String? = null,
    @Deprecated("Alltid null, bør fjernes fra kontrakt.")
    val opphorsAarsak: String? = null,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val utbetaling: List<UtbetalingMedMer>,
)

/**
 * @param dagsats Dagsats før reduksjoner.
 */
public data class VedtakUtenUtbetaling(
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int?,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val periode: Periode,
    val rettighetsType: String,
    val beregningsgrunnlag: Int,

    /** Antall barn som gir rett til barnetillegg. */
    val barnMedStonad: Int,

    /** Størrelsen på total, ugradert barnetillegg.
     *
     * Verdien er total i den forstand at den tar hensyn til antall barn.
     *
     * Den er ugradert i den forstand at hvis medlemmet har 2 barn, får 75 % AAP
     * på grunn av samordning, og barnetilleggssatsen er spesifisert i AAP-forskriften § 8 til 38 kroner,
     * så vil [barnetillegg] være 2 * 38 = 76 kroner. Altså vi har ikke redusert barnetillegget med 25% her.
     *
     * Spesifikasjon: [barnetillegg] = [barnetilleggsats] * [antallBarn].
     */
    val barnetillegg: Int,
    val kildesystem: Kilde,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
)

/**
 * @param barnetillegg Hvor mange kroner i barnetillegg.
 */
public data class UtbetalingMedMer(
    @property:Description("Reduksjon i utbetaling. Denne fås kun fra Arena,")
    val reduksjon: Reduksjon?,
    val utbetalingsgrad: Int?,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    @property:Description("Barnetillegg, _etter_ gradering.")

    /** Størrelsen på total, gradert barnetillegg.
     *
     * Verdien er total i den forstand at den tar hensyn til antall barn.
     *
     * Den er gradert i den forstand at hvis medlemmet har 2 barn, får 75 % AAP
     * på grunn av samordning, og barnetilleggssatsen er spesifisert i AAP-forskriften § 8 til 38 kroner,
     * så vil [barnetillegg] gi 2 * 38 * 0.75 = 57 kroner.
     */
    val barnetillegg: Int,
) {
    @Deprecated("Bruk barnetillegg")
    val barnetilegg: Int = barnetillegg
}

public data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: Float
)

public data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort: Int,
    val fraver: Float?
)
