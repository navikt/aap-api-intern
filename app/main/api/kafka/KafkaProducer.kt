package api.kafka

interface KafkaProducer: AutoCloseable {
    fun produce(personident: String, nyttVedtak: Boolean)
}

class KafkaProducerException(msg: String): RuntimeException(msg)