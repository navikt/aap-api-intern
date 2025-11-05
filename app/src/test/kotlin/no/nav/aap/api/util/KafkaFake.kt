package no.nav.aap.api.util

import api.kafka.KafkaProducer

class KafkaFake: KafkaProducer {
    private val messages = mutableListOf<String>()

    override fun produce(personident: String, nyttVedtak: Boolean) {
        messages.add(personident)
    }

    override fun close() {
        messages.clear()
    }

}
