package no.nav.aap.api.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AapHendelseKafkaProducerTest {

    private val objectMapper = ObjectMapper()

    @Test
    fun `vedtak-hendelse serialiseres korrekt`() {
        val record = AapHendelseRecord(ident = "12345678901", hendelse = Hendelse.VEDTAK)
        val json = objectMapper.writeValueAsString(record)

        assertThat(json).isEqualTo("""{"ident":"12345678901","hendelse":"VEDTAK"}""")
    }

    @Test
    fun `soknad-hendelse serialiseres korrekt`() {
        val record = AapHendelseRecord(ident = "12345678901", hendelse = Hendelse.SOKNAD)
        val json = objectMapper.writeValueAsString(record)

        assertThat(json).isEqualTo("""{"ident":"12345678901","hendelse":"SOKNAD"}""")
    }

    @Test
    fun `kafka-meldingen inneholder kun ident og hendelse`() {
        val record = AapHendelseRecord(ident = "12345678901", hendelse = Hendelse.VEDTAK)
        val json = objectMapper.readTree(objectMapper.writeValueAsString(record))

        assertThat(json.fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("ident", "hendelse")
    }
}
