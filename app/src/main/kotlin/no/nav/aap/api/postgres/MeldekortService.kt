package no.nav.aap.api.postgres

import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.api.kelvin.MeldekortDTO
import no.nav.aap.api.pdl.IPdlClient
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.Clock
import java.time.LocalDate

class MeldekortService(connection: DBConnection, val pdlClient: IPdlClient, clock: Clock) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), clock)

    fun hentAlleMeldekort(
        personIdentifikator: String,
        fraDato: LocalDate? = null,
        tilDato: LocalDate? = null
    ): List<MeldekortDTO> {
        val personIdenter =
            pdlClient.hentAlleIdenterForPerson(personIdentifikator).map { personIdentifikator }

        if (personIdenter.isEmpty()) return emptyList()

        return meldekortDetaljerRepository.hentAlle(personIdenter, fraDato, tilDato)
    }

    fun hentAlle(
        personIdentifikator: String,
        fom: LocalDate? = null,
        tom: LocalDate? = null
    ): List<Pair<MeldekortDTO, VedtakUtenUtbetaling?>> {
        val meldekortDetaljListe = hentAlleMeldekort(personIdentifikator, fom, tom)

        return meldekortDetaljListe.map { meldekort ->
            val vedtak = finnNyesteRelaterteVedtak(meldekort, personIdentifikator)
            Pair(meldekort, vedtak)
        }
    }


    private fun finnNyesteRelaterteVedtak(
        meldekort: MeldekortDTO, personIdentifikator: String
    ): VedtakUtenUtbetaling? {
        val meldePeriode = meldekort.meldePeriode
        // TODO finn ut hvordan man henter riktig vedtak og vedtaks-info:
        val medium = vedtakService.hentMediumFraKelvin(personIdentifikator, meldePeriode).vedtak
        val vedtak = medium.filter { it.status == "LØPENDE" }
        return vedtak.firstOrNull()
    }

}