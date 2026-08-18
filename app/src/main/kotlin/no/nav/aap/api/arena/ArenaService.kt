package no.nav.aap.api.arena

import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.api.WithMetrics
import no.nav.aap.api.intern.ArenaSakMedVedtakResponse
import no.nav.aap.api.intern.ArenaSakOppsummering
import no.nav.aap.api.intern.ArenaSakPerson
import no.nav.aap.api.intern.ArenaSakerResponse
import no.nav.aap.api.intern.ArenaVedtakDetaljer
import no.nav.aap.api.intern.ArenaVedtakfakta
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.PeriodeInkludert11_17
import no.nav.aap.api.intern.PerioderInkludert11_17Response
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.api.util.fraKontrakt
import no.nav.aap.api.util.fraKontraktUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.ManuellFordelingsgrunnlagResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.slf4j.LoggerFactory
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse as ArenaSakMedVedtakResponseV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

class ArenaService(
    private val arena: IArenaoppslagGateway, private val arenaHistorikk: IArenaoppslagGateway
) : WithMetrics {

    private val secureLog = LoggerFactory.getLogger("team-logs")

    private val log = LoggerFactory.getLogger(javaClass)

    override fun registrerMetrics(registry: MeterRegistry) {
        (arena as? WithMetrics)?.registrerMetrics(registry)
        (arenaHistorikk as? WithMetrics)?.registrerMetrics(registry)
    }

    suspend fun eksistererIAapArena(
        callId: String, personIdenter: List<String>
    ): PersonEksistererIAAPArena {
        val aapHistorikkForPerson = arenaHistorikk.hentPersonHarHistorikkIArena(
            callId, HarHistorikkRequest(personIdenter.first())
        )
        return PersonEksistererIAAPArena(aapHistorikkForPerson.harHistorikk)
    }

    suspend fun aktivitetfase(
        callId: String, vedtakRequest: InternVedtakRequest
    ): PerioderInkludert11_17Response {
        val arenaSvar = arena.hentPerioderInkludert11_17(callId, vedtakRequest)

        return PerioderInkludert11_17Response(
            perioder = arenaSvar.perioder.map { periode ->
                PeriodeInkludert11_17(
                    periode = periode.periode.let {
                        Periode(
                            it.fraOgMedDato, it.tilOgMedDato
                        )
                    },
                    aktivitetsfaseKode = periode.aktivitetsfaseKode,
                    aktivitetsfaseNavn = periode.aktivitetsfaseNavn,
                )
            })
    }

    suspend fun hentSaker(callId: String, personIdenter: List<String>): List<SakStatus.Arena> {
        val sakerRequest = SakerRequest(personIdenter)
        return arena.hentSakerByFnr(callId, sakerRequest).map {
            arenaSakStatusTilDomene(it).also { arenaSak ->
                if (arenaSak.periode().fraOgMedDato == null) {
                    secureLog.info("Arena-sak med null fraDato. Til-dato ${arenaSak.periode().tilOgMedDato} Status: ${arenaSak.statusKode}")
                }
            }
        }
    }

    private fun arenaSakStatusTilDomene(it: no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus) =
        SakStatus.Arena(
            sakId = it.sakId, statusKode = when (it.statusKode) {
                Status.AVSLU -> no.nav.aap.api.intern.ArenaStatus.AVSLU
                Status.FORDE -> no.nav.aap.api.intern.ArenaStatus.FORDE
                Status.GODKJ -> no.nav.aap.api.intern.ArenaStatus.GODKJ
                Status.INNST -> no.nav.aap.api.intern.ArenaStatus.INNST
                Status.IVERK -> no.nav.aap.api.intern.ArenaStatus.IVERK
                Status.KONT -> no.nav.aap.api.intern.ArenaStatus.KONT
                Status.MOTAT -> no.nav.aap.api.intern.ArenaStatus.MOTAT
                Status.OPPRE -> no.nav.aap.api.intern.ArenaStatus.OPPRE
                Status.REGIS -> no.nav.aap.api.intern.ArenaStatus.REGIS
                Status.UKJENT -> no.nav.aap.api.intern.ArenaStatus.UKJENT
            }, periode = Periode(
                it.periode.fraOgMedDato, it.periode.tilOgMedDato
            )
        )

    suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): List<Periode> {
        return arena.hentPerioder(callId, vedtakRequest).perioder
    }

    suspend fun hentSakerForPerson(
        callId: String, personidentifikator: String
    ): ArenaSakerResponse {
        return arena.hentSakerForPerson(callId, SakerRequestV1(personidentifikator)).toResponse()
    }

    suspend fun hentManuellFordelingsgrunnlag(
        callId: String, personidentifikator: String
    ): ManuellFordelingsgrunnlagResponse? {
        return arena.hentManuellFordelingsgrunnlag(callId, personidentifikator)
    }

    suspend fun hentVedtakUtenUtbetaling(
        callId: String, vedtakRequest: InternVedtakRequest
    ): List<InternVedtakUtenUtbetaling> {
        return maksimum(callId, vedtakRequest).vedtak.map { it.fraKontraktUtenUtbetaling() }
    }

    suspend fun hentVedtak(callId: String, vedtakRequest: InternVedtakRequest): List<InternVedtak> {
        return maksimum(callId, vedtakRequest).fraKontrakt().vedtak
    }


    private suspend fun maksimum(
        callId: String, vedtakRequest: InternVedtakRequest
    ): Maksimum {
        val hentMaksimum = arena.hentMaksimum(callId, vedtakRequest)
        val medKodefilter = hentMaksimum.let {
            it.copy(vedtak = it.vedtak.filter { vedtak ->
                // Gjenskaper filter i Arenaoppslag for å kunne
                // https://github.com/navikt/aap-arenaoppslag/blob/0506908b60c81103882386a3ed8572bb5e7d17bf/app/src/main/kotlin/no/nav/aap/arenaoppslag/database/MaksimumRepository.kt#L210
                /*
                           AND vedtaktypekode IN ('O', 'E', 'G', 'S')
                           AND vedtakstatuskode IN ('IVERK', 'AVSLU')
                           AND (fra_dato <= til_dato OR til_dato IS NULL)
                           AND (til_dato >= ? OR til_dato IS NULL)
                           AND fra_dato <= ?
                 */
                val fraOgMedDato = vedtak.periode.fraOgMedDato
                val datoBetingelser =
                    fraOgMedDato == null || vedtak.periode.tilOgMedDato == null || fraOgMedDato <= vedtak.periode.tilOgMedDato

                val vedtaktypeBetingelser = vedtak.vedtaksTypeKode in listOf("O", "E", "G", "S")

                val vedtakstatusKode = vedtak.status in listOf("IVERK", "AVSLU")

                datoBetingelser and vedtaktypeBetingelser and vedtakstatusKode
            })
        }

        // Midlertidig logg. Er denne noengang null?
        if (hentMaksimum.vedtak.any { it.utfallkode == null }) {
            log.warn("Maksimum-vedtak med null utfallkode.")
        }

        val original = hentMaksimum.let {
            it.copy(vedtak = it.vedtak.filter { vedtak ->
                val fraOgMedDato = vedtak.periode.fraOgMedDato
                fraOgMedDato == null || vedtak.periode.tilOgMedDato == null || fraOgMedDato <= vedtak.periode.tilOgMedDato
            })
        }

        if (medKodefilter != original) {
            val størsteTilDatoOriginal = original.vedtak.maxOfOrNull { it.periode.tilOgMedDato ?: LocalDate.MIN }
            val størsteTilDatoMedKodefilter = medKodefilter.vedtak.maxOfOrNull { it.periode.tilOgMedDato ?: LocalDate.MIN }
            log.warn("Kodefilter er forskjellig fra SQL. Lengde: ${medKodefilter.vedtak.size} vs ${original.vedtak.size}. Største til dato: $størsteTilDatoMedKodefilter vs $størsteTilDatoOriginal.")
        }
        return original
    }

    suspend fun hentArenaSakMedVedtak(callId: String, sakId: String): ArenaSakMedVedtakResponse? =
        arena.hentArenaSakMedVedtak(callId, sakId)?.toInternResponse()

}


private fun SakerResponse.toResponse() = ArenaSakerResponse(
    saker = saker.map { sak ->
        ArenaSakOppsummering(
            sakId = sak.sakId,
            lopenummer = sak.lopenummer,
            aar = sak.aar,
            antallVedtak = sak.antallVedtak,
            statuskode = sak.statuskode,
            statusnavn = sak.statusnavn,
            sakstype = sak.sakstype,
            regDato = sak.regDato,
            avsluttetDato = sak.avsluttetDato,
        )
    })

private fun ArenaSakMedVedtakResponseV1.toInternResponse() = ArenaSakMedVedtakResponse(
    sakId = sakId,
    opprettetAar = opprettetAar,
    lopenr = lopenr,
    person = ArenaSakPerson(
        personId = person.personId,
        fodselsnummer = person.fodselsnummer,
        fornavn = person.fornavn,
        etternavn = person.etternavn,
    ),
    statuskode = statuskode,
    statusnavn = statusnavn,
    registrertDato = registrertDato,
    avsluttetDato = avsluttetDato,
    vedtak = vedtak.map { v ->
        ArenaVedtakDetaljer(
            vedtakId = v.vedtakId,
            lopenrvedtak = v.lopenrvedtak,
            statusKode = v.statusKode,
            statusNavn = v.statusNavn,
            vedtaktypeKode = v.vedtaktypeKode,
            vedtaktypeNavn = v.vedtaktypeNavn,
            aktivitetsfaseKode = v.aktivitetsfaseKode,
            aktivitetsfaseNavn = v.aktivitetsfaseNavn,
            fraOgMed = v.fraOgMed,
            tilDato = v.tilDato,
            rettighetkode = v.rettighetkode,
            rettighetnavn = v.rettighetnavn,
            utfallkode = v.utfallkode,
            begrunnelse = v.begrunnelse,
            saksbehandler = v.saksbehandler,
            beslutter = v.beslutter,
            relatertVedtak = v.relatertVedtak,
            fakta = v.fakta.map { f ->
                ArenaVedtakfakta(
                    kode = f.kode,
                    navn = f.navn,
                    verdi = f.verdi,
                    registrertDato = f.registrertDato,
                )
            },
        )
    },
)