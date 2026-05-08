package no.nav.aap.api.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class AapHendelseRecord(
    val fnr: String,
    val hendelse: String,
)

class AapHendelseKafkaProducer(
    config: KafkaConfig,
    private val topic: String,
    private val closeTimeout: Duration,
) : AutoCloseable {
    private val producer = KafkaFactory.createProducer("aap-api-hendelse", config)
    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

    fun produce(fnr: String, hendelse: Enum<*>) {
        val json = objectMapper.writeValueAsString(AapHendelseRecord(fnr, hendelse.name))
        val record = ProducerRecord(topic, fnr, json)
        producer.send(record) { metadata, err ->
            if (err != null) {
                logger.error("Klarte ikke sende hendelse til kafka, metadata: $metadata", err)
                throw KafkaProducerException("Klarte ikke sende hendelse til kafka", err)
            }
        }.get()
    }

    override fun close() = producer.close(closeTimeout.toJavaDuration())
}
