package no.nav.aap.api.kafka

object AapHendelseProducerHolder {

    private var producer: AapHendelseKafkaProducer? = null

    fun set(producer: AapHendelseKafkaProducer) {
        this.producer = producer
    }

    fun producer(): AapHendelseKafkaProducer {
        return requireNotNull(producer) { "AapHendelseKafkaProducer was not set." }
    }
}
