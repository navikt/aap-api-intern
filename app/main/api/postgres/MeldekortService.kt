package api.postgres

import api.kelvin.MeldekortDTO
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class MeldekortService(connection: DBConnection) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), LocalDate.now())

    fun hentAlle(personIdentifikator: String, fraDato: LocalDate? = null): List<Pair<MeldekortDTO, Vedtak>> {
        // TODO bør hente alle personIdent fra PDL, og bruke dem i db-query
        val meldekortDetaljListe = meldekortDetaljerRepository.hentAlle(personIdentifikator, fraDato)

        return meldekortDetaljListe.map { meldekort ->
            val vedtak = finnRelatertVedtak(meldekort, personIdentifikator)

            Pair(meldekort, vedtak)
        }

    }

    private fun finnRelatertVedtak(
        meldekort: MeldekortDTO, personIdentifikator: String
    ): Vedtak {
        val meldePeriode = meldekort.meldePeriode
        // TODO finn ut hvordan man henter riktig vedtak og vedtaks-info:
        val maksimum = vedtakService.hentMaksimum(personIdentifikator, meldePeriode)
        val vedtak = maksimum.vedtak.first()
        return vedtak
    }

}