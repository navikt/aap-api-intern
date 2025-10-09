package api.util

import api.kafka.KafkaProducer

class KafkaFake: KafkaProducer {
    private val messages = mutableListOf<String>()

    override fun produce(personident: String, nyttVedtak: Boolean) {
        messages.add(personident)
    }

    override fun close() {
        messages.clear()
    }

    fun hasProduced(personident: String): Boolean {
        return messages.contains(personident)
    }
}
