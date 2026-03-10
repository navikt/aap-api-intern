package no.nav.aap.api.kelvin

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import java.net.URI

data class OppgaveGatewayConfig(
    val baseUrl: URI = URI.create(requiredConfigForKey("INTEGRASJON_OPPGAVE_URL")),
    val config: ClientConfig = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_OPPGAVE_SCOPE"))
)