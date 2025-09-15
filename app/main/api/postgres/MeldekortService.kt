package api.postgres

import api.kelvin.MeldekortDTO
import api.pdl.IPdlClient
import api.pdl.PdlClient
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class MeldekortService(connection: DBConnection, val pdlClient: IPdlClient) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), LocalDate.now())

    fun hentAlleMeldekort(personIdentifikator: String, fraDato: LocalDate? = null): List<MeldekortDTO> {
        val personIdenter = pdlClient.hentAlleIdenterForPerson(personIdentifikator).map { personIdentifikator }
        return meldekortDetaljerRepository.hentAlle(personIdenter, fraDato)
    }

    fun hentAlle(personIdentifikator: String, fraDato: LocalDate? = null): List<Pair<MeldekortDTO, Vedtak>> {

        val personIdenter = pdlClient.hentAlleIdenterForPerson(personIdentifikator).map { personIdentifikator }
        val meldekortDetaljListe = meldekortDetaljerRepository.hentAlle(personIdenter, fraDato)

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