package api.postgres

import api.kelvin.DetaljertMeldekortListeDTO
import io.ktor.network.sockets.Connection
import org.slf4j.LoggerFactory

class MeldekortRepository(private val connection: Connection) {
    private val log = LoggerFactory.getLogger(MeldekortRepository::class.java)

    fun lagreMeldekort(meldekortList: DetaljertMeldekortListeDTO){

    }



}