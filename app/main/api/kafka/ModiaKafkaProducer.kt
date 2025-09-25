package api.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class ModiaKafkaProducer(config: KafkaConfig) : KafkaProducer, AutoCloseable {
    private val producer = KafkaFactory.createProducer("aap-api", config)
    private val topic = "obo.ytelse-v1"
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun produce(personident: String) {
        val record = createRecord(personident)
        producer.send(record) { metadata, err ->
            if (err != null) {
                logger.error("Klarte ikke varsle hendelse for bruker", err)
                throw KafkaProducerException("Klarte ikke valse hendelse for bruker")
            }
        }.get() // Blocking call to ensure the message is sent
    }

    private fun createRecord(personident: String): ProducerRecord<String, String> {
        val json = """{
  personId: String; // meldings-key
  meldingstype: OPPRETT | OPPDATER;
  ytelsestype: AAP;
  kildesystem: KELVIN; 
}"""//TODO: få opp riktig JSON her

        return ProducerRecord(topic, personident, json)
    }

    override fun close() = producer.close()
}
