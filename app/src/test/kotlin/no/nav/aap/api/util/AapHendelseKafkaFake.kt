package no.nav.aap.api.util

import no.nav.aap.api.kafka.AapHendelseProducer

class AapHendelseKafkaFake : AapHendelseProducer {
    private val messages = mutableListOf<Pair<String, String>>()

    override fun produce(fnr: String, hendelse: Enum<*>) {
        messages.add(fnr to hendelse.name)
    }

    override fun close() {
        messages.clear()
    }
}
