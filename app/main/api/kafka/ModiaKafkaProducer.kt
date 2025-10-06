package api.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class ModiaKafkaProducer(config: KafkaConfig) : KafkaProducer, AutoCloseable {
    private val producer = KafkaFactory.createProducer("aap-api", config)
    private val topic = "obo.ytelser-v1"
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun produce(personident: String, meldingstype: ModiaRecord.Meldingstype?) {
        val record = createRecord(personident, meldingstype?: ModiaRecord.Meldingstype.OPPDATER)
        producer.send(record) { metadata, err ->
            if (err != null) {
                logger.error("Klarte ikke varsle hendelse for bruker", err)
                throw KafkaProducerException("Klarte ikke valse hendelse for bruker")
            } else {
                logger.info("Sendte melding til topic ${metadata.topic()}")
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
