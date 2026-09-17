package no.nav.aap.api.util

import no.nav.aap.api.kafka.arbeidsoppfølging.ArbeidsoppfølgingProducer
import no.nav.aap.api.kafka.arbeidsoppfølging.ArbeidsoppfølgingRecord

class ArbeidsoppfølgingKafkaFake : ArbeidsoppfølgingProducer {
    private val messages = mutableListOf<Pair<String, ArbeidsoppfølgingRecord>>()

    override fun produce(personident: String) {
        messages.add(
            personident to ArbeidsoppfølgingRecord(
                personident = personident
            )
        )
    }

    override fun close() {
        messages.clear()
    }
}