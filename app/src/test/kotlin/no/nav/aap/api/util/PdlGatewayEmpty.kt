package no.nav.aap.api.util

import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.pdl.PdlIdent

class PdlGatewayEmpty : IPdlGateway {
    override fun hentAlleIdenterForPerson(personIdent: String): List<PdlIdent> {
        return listOf(PdlIdent(personIdent, historisk = false, gruppe = "FOLKEREGISTERIDENT"))
    }
}