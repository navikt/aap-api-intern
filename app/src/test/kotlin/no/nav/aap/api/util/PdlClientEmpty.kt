package no.nav.aap.api.util

import no.nav.aap.api.pdl.IPdlClient
import no.nav.aap.api.pdl.PdlIdent

class PdlClientEmpty : IPdlClient {
    override fun hentAlleIdenterForPerson(personIdent: String): List<PdlIdent> {
        return listOf(PdlIdent(personIdent, historisk = false, gruppe = "FOLKEREGISTERIDENT"))
    }
}