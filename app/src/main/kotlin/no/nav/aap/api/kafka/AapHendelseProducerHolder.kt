package no.nav.aap.api.kafka

object AapHendelseProducerHolder {

    private var producer: AapHendelseProducer? = null

    fun set(producer: AapHendelseProducer) {
        this.producer = producer
    }

    fun producer(): AapHendelseProducer {
        return requireNotNull(producer) { "AapHendelseProducer was not set." }
    }
}
