package no.nav.aap.api.motor.jobber

import no.nav.aap.api.kafka.arbeidsoppfølging.arbeidsoppfølgingProducerHolder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører


data class ArbeidsoppfølgingHendelsePayload(
    val personident: String
)

class SendArbeidsoppfølgingHendelseUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {
        if (Miljø.erProd()) {
            // skrudd av i prod inntil videre
            return
        }
        val payload = DefaultJsonMapper.fromJson<ArbeidsoppfølgingHendelsePayload>(input.payload())
        arbeidsoppfølgingProducerHolder.produce(payload.personident)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører = SendArbeidsoppfølgingHendelseUtfører()

        override fun type(): String = "aap.arbeidsoppfølging.hendelse.send"

        override fun navn(): String = "Send hendelse til arbeidsoppfølging"

        override fun beskrivelse(): String = "Sender hendelse for hver nye søknad i Kelvin til Modia Arbeidsoppfølging"
    }
}