package no.nav.aap.api.kelvin

import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.MeldekortDetaljerRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class MeldekortService(
    connection: DBConnection,
    val pdlGateway: IPdlGateway
) {
    val meldekortDetaljerRepository = MeldekortDetaljerRepository(connection)

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

}