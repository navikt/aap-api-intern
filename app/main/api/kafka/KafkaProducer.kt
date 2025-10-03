package api.kafka

interface KafkaProducer: AutoCloseable {
    fun produce(personident: String, meldingstype: ModiaRecord.Meldingstype?)
}

class KafkaProducerException(msg: String): RuntimeException(msg)