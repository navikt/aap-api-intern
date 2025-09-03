package api.postgres

import api.kelvin.MeldekortDTO
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class MeldekortService(connection: DBConnection) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), LocalDate.now())

    fun hentHvisEksisterer(personIdentifikator: String, fraDato: LocalDate? = null): List<Pair<MeldekortDTO, Vedtak>>? {
        val meldekortDetaljListe = meldekortDetaljerRepository.hentHvisEksisterer(personIdentifikator, fraDato)

        return meldekortDetaljListe?.meldekort?.map { meldekort ->
            // TODO finn ut hvordan man henter riktig vedtak og vedtaks-info
            val meldePeriode = meldekort.meldePeriode
            val maksimum = vedtakService.hentMaksimum(personIdentifikator, meldePeriode)
            val vedtak = maksimum.vedtak.first()

            Pair(meldekort, vedtak)
        }

    }

}