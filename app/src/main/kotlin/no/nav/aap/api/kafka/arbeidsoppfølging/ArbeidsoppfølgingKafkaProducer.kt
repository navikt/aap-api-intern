package no.nav.aap.api.kafka.arbeidsoppfølging

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.api.ArbeidsoppfølgingConfig
import no.nav.aap.api.kafka.KafkaConfig
import no.nav.aap.api.kafka.KafkaFactory
import no.nav.aap.api.kafka.KafkaProducerException
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.toJavaDuration

interface ArbeidsoppfølgingProducer : AutoCloseable {
    fun produce(personident: String)
}

class ArbeidsoppfølgingKafkaProducer(
    config: KafkaConfig,
    arbeidsoppfølgingConfig: ArbeidsoppfølgingConfig,
    private val closeTimeout: Duration
) : ArbeidsoppfølgingProducer, AutoCloseable {
    private val producer = KafkaFactory.createProducer("aap-api", config)
    private val topic = arbeidsoppfølgingConfig.topic
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun produce(personident: String) {
        val record = createRecord(personident)

        producer.send(record) { metadata, err ->
            if (err != null) {
                logger.error("Klarte ikke varsle arbeidsoppfølging, metadata: $metadata", err)
                throw KafkaProducerException("Klarte ikke varsle arbeidsoppfølging", err)
            }
        }.get()
    }

    private fun createRecord(
        personident: String,
    ): ProducerRecord<String, String> {
        val json = ObjectMapper().writeValueAsString(ArbeidsoppfølgingRecord(personident))

        return ProducerRecord(topic, personident, json)
    }

    override fun close() = producer.close(closeTimeout.toJavaDuration())
}

data class ArbeidsoppfølgingRecord(
    val personident: String,
    val aarsak: String = "AAP_SØKNAD",
    val kilde: String = "KELVIN",
    val registrant: Registrant = Registrant()
)

data class Registrant(
    val type: String = "SYSTEM",
    val opprettetAv: String = "KELVIN"
)
