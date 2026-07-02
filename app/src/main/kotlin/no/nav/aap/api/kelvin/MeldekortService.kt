package no.nav.aap.api.kelvin

import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.MeldekortDetaljerRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.time.Clock
import java.time.LocalDate

class MeldekortService(
    connection: DBConnection,
    val pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone()
) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)
    val vedtakService = VedtakService(BehandlingsRepository(connection), clock)

    fun hentAlleMeldekort(
        personIdentifikator: String,
        fraDato: LocalDate? = null,
        tilDato: LocalDate? = null
    ): List<Meldekort> {
        val personIdenter =
            pdlGateway.hentAlleIdenterForPerson(personIdentifikator).map { it.ident }

        if (personIdenter.isEmpty()) return emptyList()

        return meldekortDetaljerRepository.hentAlle(personIdenter, fraDato, tilDato)
    }

    fun hentAlleMeldekortMedMeldeperiodeEllerMottattIPeriode(
        personIdentifikator: String,
        fraDato: LocalDate? = null,
        tilDato: LocalDate? = null
    ): List<Meldekort> {
        val personIdenter =
            pdlGateway.hentAlleIdenterForPerson(personIdentifikator).map { it.ident }
                .ifEmpty { return emptyList() }

        return meldekortDetaljerRepository.hentAlleMedMeldeperiodeEllerMottattIPeriode(
            personIdenter,
            fraDato,
            tilDato
        )
    }

    /**
     * Henter alle meldekort for en person. Merk at denne også returnerer meldekort
     * for perioder uten rett (ennå). F.eks mens førstegangsbehandlingen er under behandling.
     */
    fun hentAlle(
        personIdentifikator: String,
        fom: LocalDate? = null,
        tom: LocalDate? = null
    ): List<Pair<Meldekort, List<Pair<Periode, TilkjentYtelse>>>> {
        val meldekortDetaljListe = hentAlleMeldekort(personIdentifikator, fom, tom)

        return meldekortDetaljListe.map { meldekort ->
            val vedtak = utbetalingForMeldekort(meldekort)
            Pair(meldekort, vedtak)
        }
    }

    private fun utbetalingForMeldekort(meldekort: Meldekort): List<Pair<Periode, TilkjentYtelse>> {
        return vedtakService.tilkjentYtelseForPeriode(meldekort.personIdent, meldekort.meldePeriode)
            .map { it.periode to it.verdi }
    }

}