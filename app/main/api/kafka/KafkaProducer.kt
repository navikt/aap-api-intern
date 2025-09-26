package api.kafka

interface KafkaProducer: AutoCloseable {
    fun produce(personident: String)
}

class KafkaProducerException(msg: String): RuntimeException(msg)