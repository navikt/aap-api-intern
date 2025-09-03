package api.postgres

import api.kelvin.MeldekortDetaljListeDTO
import no.nav.aap.komponenter.dbconnect.DBConnection

class MeldekortDetaljerRepository(private val connection: DBConnection) {

    fun lagre(meldekortListe: MeldekortDetaljListeDTO) {
        TODO()
        // dersom lagringen feiler, må vi kaste exception for at transaksjonen skal rulles tilbake
    }

}