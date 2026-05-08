package no.nav.aap.api.motor.jobber

import no.nav.aap.api.kafka.AapHendelseProducerHolder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

enum class Hendelse {
    FORSTEGANGSVEDTAK,
    ENDRINGSVEDTAK,
    STANSVEDTAK,
    SOKNAD,
}

data class AapHendelsePayload(
    val fnr: String,
    val hendelse: Hendelse,
)

class SendAapHendelseUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload = DefaultJsonMapper.fromJson<AapHendelsePayload>(input.payload())
        AapHendelseProducerHolder.producer().produce(payload.fnr, payload.hendelse)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører = SendAapHendelseUtfører()

        override fun type(): String = "aap.hendelse.send"

        override fun navn(): String = "Send AAP-hendelse"

        override fun beskrivelse(): String = "Sender AAP-hendelse på eget Kafka-topic"
    }
}
