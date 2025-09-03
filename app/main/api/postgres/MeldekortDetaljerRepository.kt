package api.postgres

import api.kelvin.MeldekortDetaljListeDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDate

class MeldekortDetaljerRepository(private val connection: DBConnection) {

    fun lagre(meldekortListe: MeldekortDetaljListeDTO) {
        TODO()
        // dersom lagringen feiler, må vi kaste exception for at transaksjonen skal rulles tilbake
    }

    fun hentHvisEksisterer(personIdentifikator: String, fraOgMed: LocalDate? = null): MeldekortDetaljListeDTO? {
        val iMorgen = LocalDate.now().plusDays(1)
        require(fraOgMed == null || fraOgMed.isBefore(iMorgen)) {
            "Kan ikke hente meldekort fra og med en fremtidig dato"
        }
        TODO()
    }

}