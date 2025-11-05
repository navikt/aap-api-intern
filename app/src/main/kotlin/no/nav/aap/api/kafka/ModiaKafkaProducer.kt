package no.nav.aap.api.kafka

import no.nav.aap.api.ModiaConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class ModiaKafkaProducer(config: KafkaConfig, modiaConfig: ModiaConfig) : KafkaProducer, AutoCloseable {
    private val producer = KafkaFactory.createProducer("aap-api", config)
    private val topic = modiaConfig.topic
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun produce(personident: String, nyttVedtak: Boolean) {

        val meldingstype = if (nyttVedtak) ModiaRecord.Meldingstype.OPPRETT else ModiaRecord.Meldingstype.OPPDATER

        val record = createRecord(personident, meldingstype)

        producer.send(record) { metadata, err ->
            if (err != null) {
                logger.error("Klarte ikke varsle hendelse for bruker, metadata: $metadata", err)
                throw KafkaProducerException("Klarte ikke varsle hendelse for bruker", err)
            }
        }.get() // Blocking call to ensure the message is sent
    }

    private fun createRecord(personident: String, type : ModiaRecord.Meldingstype): ProducerRecord<String, String> {
        val json = ObjectMapper().writeValueAsString(ModiaRecord(personident, type))

        return ProducerRecord(topic, personident, json)
    }

    override fun close() = producer.close()
}

data class ModiaRecord(
    val personId: String,
    val meldingstype: Meldingstype = Meldingstype.OPPRETT,
    val ytelsestype: String = "AAP", // AAP;
    val kildesystem: String = "KELVIN", // KELVIN;
){
    enum class Meldingstype { OPPRETT, OPPDATER }
}
