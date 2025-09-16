package api.postgres

import api.kelvin.MeldekortDTO
import api.pdl.IPdlClient
import api.pdl.PdlClient
import no.nav.aap.api.intern.Medium
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class MeldekortService(connection: DBConnection, val pdlClient: IPdlClient) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), LocalDate.now())

    fun hentAlleMeldekort(personIdentifikator: String, fraDato: LocalDate? = null, tilDato: LocalDate? = null): List<MeldekortDTO> {
        val personIdenter = pdlClient.hentAlleIdenterForPerson(personIdentifikator).map { personIdentifikator }
        return meldekortDetaljerRepository.hentAlle(personIdenter, fraDato, tilDato)
    }

    fun hentAlle(personIdentifikator: String, fom: LocalDate? = null, tom : LocalDate? = null): List<Pair<MeldekortDTO, VedtakUtenUtbetaling>> {
        val meldekortDetaljListe = hentAlleMeldekort(personIdentifikator, fom, tom)

        return meldekortDetaljListe.map { meldekort ->
            val vedtak = finnNyesteRelaterteVedtak(meldekort, personIdentifikator)
            Pair(meldekort, vedtak)
        }
    }


    private fun finnNyesteRelaterteVedtak(
        meldekort: MeldekortDTO, personIdentifikator: String
    ): VedtakUtenUtbetaling {
        val meldePeriode = meldekort.meldePeriode
        // TODO finn ut hvordan man henter riktig vedtak og vedtaks-info:
        val medium = vedtakService.hentMediumFraKelvin(personIdentifikator, meldePeriode).vedtak
        val vedtak = medium.filter { it.status =="LØPENDE" }
        return vedtak.first()
    }

}