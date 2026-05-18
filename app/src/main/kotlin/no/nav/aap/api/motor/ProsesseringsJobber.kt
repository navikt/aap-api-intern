package no.nav.aap.api.motor

import no.nav.aap.api.motor.jobber.SendAapHendelseUtfører
import no.nav.aap.api.motor.jobber.SendModiaHendelseUtfører
import no.nav.aap.motor.Jobb

/**
* Alle oppgavene som skal utføres i systemet
*/
object ProsesseringsJobber {
    fun alle(): List<Jobb> {
        return listOf(
            SendAapHendelseUtfører,
            SendModiaHendelseUtfører,
        )
    }
}