package no.nav.aap.api.motor.jobber

import no.nav.aap.api.kafka.modiaProducerHolder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

data class ModiaHendelsePayload(
    val fnr: String,
    val nyttVedtak: Boolean,
)

class SendModiaHendelseUtfører : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val payload = DefaultJsonMapper.fromJson<ModiaHendelsePayload>(input.payload())
        modiaProducerHolder.produce(payload.fnr, payload.nyttVedtak)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører = SendModiaHendelseUtfører()

        override fun type(): String = "modia.hendelse.send"

        override fun navn(): String = "Send Modia-hendelse"

        override fun beskrivelse(): String = "Sender vedtakhendelse til Modia sitt Kafka-topic"
    }
}
