package no.nav.aap.api.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.api.AapHendelseConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.toJavaDuration

interface AapHendelseProducer : AutoCloseable {
    fun produce(fnr: String, hendelse: Hendelse)
}

data class AapHendelseRecord(
    val fnr: String,
    val hendelse: String,
)

class AapHendelseKafkaProducer(
    config: KafkaConfig,
    hendelseConfig: AapHendelseConfig,
    private val closeTimeout: Duration,
) : AapHendelseProducer {
    private val producer = KafkaFactory.createProducer("aap-api-hendelse", config)
    private val topic = hendelseConfig.topic
    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun produce(fnr: String, hendelse: Hendelse) {
        val json = objectMapper.writeValueAsString(AapHendelseRecord(fnr, hendelse.name))
        val record = ProducerRecord(topic, fnr, json)
        logger.info("Sender hendelse $hendelse til kafka-topic $topic")
        producer.send(record) { metadata, err ->
            if (err != null) {
                logger.error("Klarte ikke sende hendelse til kafka, metadata: $metadata", err)
                throw KafkaProducerException("Klarte ikke sende hendelse til kafka", err)
            }
        }.get()
    }

    override fun close() = producer.close(closeTimeout.toJavaDuration())
}
