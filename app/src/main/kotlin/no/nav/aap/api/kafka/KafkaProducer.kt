package no.nav.aap.api.kafka

interface KafkaProducer: AutoCloseable {
    fun produce(personident: String, nyttVedtak: Boolean)
}

class KafkaProducerException(msg: String, error: Exception): RuntimeException(msg,error)