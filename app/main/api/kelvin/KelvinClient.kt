package api.kelvin

import api.KelvinConfig
import no.nav.aap.api.intern.KelvinPeriode
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

class KelvinClient(
    private val kelvinConfig: KelvinConfig
) {
    private val uri = kelvinConfig.proxyBaseUrl
    private val config = ClientConfig(scope = kelvinConfig.scope)

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun hentMeldekortPerioder(req: InternVedtakRequest): List<KelvinPeriode> {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = req
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create(uri).resolve("/api/datadeling/perioder/meldekort"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å hente meldekortPerioder fra Kelvin: ${e.message}")
        }
    }

    fun hentMaksimum(req: InternVedtakRequest): List<Vedtak> {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = req
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create(uri).resolve("/api/datadeling/vedtak"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å hente Maksimum fra Kelvin: ${e.message}")
        }
    }
}