package no.nav.aap.api.postgres

import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.api.kelvin.Meldekort
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.time.Clock
import java.time.LocalDate
import no.nav.aap.api.kelvin.DsopService

class MeldekortService(connection: DBConnection, val pdlGateway: IPdlGateway, clock: Clock) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), clock)
    val dsopService = DsopService(connection)

    private fun hentAlleMeldekort(
        personIdentifikator: String,
        fraDato: LocalDate? = null,
        tilDato: LocalDate? = null
    ): List<Meldekort> {
        val personIdenter =
            pdlGateway.hentAlleIdenterForPerson(personIdentifikator).map { it.ident }

        if (personIdenter.isEmpty()) return emptyList()

        return meldekortDetaljerRepository.hentAlle(personIdenter, fraDato, tilDato)
    }

    /**
     * Henter alle meldekort for en person. Merk at denne også returnerer meldekort
     * for perioder uten rett (ennå). F.eks mens førstegangsbehandlingen er under behandling.
     */
    fun hentAlle(
        personIdentifikator: String,
        fom: LocalDate? = null,
        tom: LocalDate? = null
    ): List<Pair<Meldekort, VedtakUtenUtbetaling?>> {
        val meldekortDetaljListe = hentAlleMeldekort(personIdentifikator, fom, tom)

        return meldekortDetaljListe.map { meldekort ->
            val vedtak = finnNyesteRelaterteVedtak(meldekort, personIdentifikator)
            Pair(meldekort, vedtak)
        }
    }

    fun hentAlleMeldekortMedRett(
        personIdentifikator: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<Meldekort> {
        val kelvinVedtak = dsopService.hentDsopVedtak(
            personIdentifikator,
            Periode(fom, tom)
        )

        val meldekortListe = hentAlleMeldekort(personIdentifikator, fom, tom)

        val filtrerteMeldekort = meldekortListe.filter { meldekort ->
            kelvinVedtak.any {
                val periode = Periode(it.virkningsperiode.fom, it.virkningsperiode.tom)
                periode.overlapper(
                    Periode(
                        meldekort.meldePeriode.fom,
                        meldekort.meldePeriode.tom
                    )
                )
            }
        }

        return filtrerteMeldekort
    }


    private fun finnNyesteRelaterteVedtak(
        meldekort: Meldekort, personIdentifikator: String
    ): VedtakUtenUtbetaling? {
        val meldePeriode = meldekort.meldePeriode
        // TODO finn ut hvordan man henter riktig vedtak og vedtaks-info:
        val medium = vedtakService.hentMediumFraKelvin(personIdentifikator, meldePeriode).vedtak
        val vedtak = medium.filter { it.status == "LØPENDE" }
        return vedtak.firstOrNull()
    }
}

