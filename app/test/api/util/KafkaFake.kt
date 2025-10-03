package api.util

import api.kafka.KafkaProducer

class KafkaFake: KafkaProducer {
    private val messages = mutableListOf<String>()

    override fun produce(personident: String, meldingstype: api.kafka.ModiaRecord.Meldingstype?) {
        messages.add(personident)
    }

    override fun close() {
        messages.clear()
    }

    fun hasProduced(personident: String): Boolean {
        return messages.contains(personident)
    }
}
