package api.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import java.util.*



class KafkaFactory private constructor() {
    companion object {
        fun<T:Any> createProducer(clientId: String, config: KafkaConfig): KafkaProducer<String, T> {
            fun properties(): Properties = Properties().apply {
                this[CommonClientConfigs.CLIENT_ID_CONFIG] = clientId
                this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = config.brokers
                this[ProducerConfig.ACKS_CONFIG] = "all"
                this[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "5"
                this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
                this[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
                this[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = config.truststorePath
                this[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = config.credstorePsw
                this[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
                this[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = config.keystorePath
                this[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = config.credstorePsw
                this[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = config.credstorePsw
                this[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
            }

            return KafkaProducer(properties(), Serdes.StringSerde().serializer(), JacksonSerializer<T>())
        }
    }
}

class JacksonSerializer<T:Any>:Serializer<T>{

    companion object {
        internal val jackson: ObjectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun serialize(topic: String?, data: T): ByteArray = jackson.writeValueAsBytes(data)
    override fun close() {}
}