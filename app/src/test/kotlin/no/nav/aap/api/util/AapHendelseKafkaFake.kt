package no.nav.aap.api.util

import no.nav.aap.api.kafka.AapHendelseProducer
import no.nav.aap.api.kafka.Hendelse

class AapHendelseKafkaFake : AapHendelseProducer {
    private val messages = mutableListOf<Pair<String, Hendelse>>()

    fun sentMessages(): List<Pair<String, Hendelse>> = messages.toList()

    override fun produce(fnr: String, hendelse: Hendelse) {
        messages.add(fnr to hendelse)
    }

    override fun close() {
        messages.clear()
    }
}
