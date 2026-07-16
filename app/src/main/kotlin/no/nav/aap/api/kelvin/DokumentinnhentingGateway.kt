package no.nav.aap.api.kelvin

import behandlingsflyt.DialogmeldingEksistererDto
import no.nav.aap.api.Metrics
import java.net.URI
import java.util.UUID
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider

class DokumentinnhentingGateway {
    private val baseUrl = URI.create(requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_URL"))
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_SCOPE"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        prometheus = Metrics.prometheus,
        tokenProvider = AzureM2MTokenProvider,
    )

    fun dialogmeldingEksisterer(
        dialogmeldingId: UUID,
    ): DialogmeldingEksistererDto {
        return requireNotNull(
            client.get<DialogmeldingEksistererDto>(
                uri = baseUrl.resolve("/dialogmelding/$dialogmeldingId/eksisterer"),
                request = GetRequest(
                    additionalHeaders = listOf(
                        Header("Accept", "application/json")
                    )
                ),
            )
        )
    }
}
