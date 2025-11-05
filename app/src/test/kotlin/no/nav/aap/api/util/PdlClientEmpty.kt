package no.nav.aap.api.util

import api.pdl.IPdlClient
import api.pdl.PdlIdent

class PdlClientEmpty : IPdlClient {
    override fun hentAlleIdenterForPerson(personIdent: String): List<PdlIdent> {
        return listOf(PdlIdent(personIdent, historisk = false, gruppe = "FOLKEREGISTERIDENT"))
    }
}