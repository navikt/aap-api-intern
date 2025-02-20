package api.kelvin

import api.KelvinConfig
import api.maksimum.KelvinPeriode
import api.maksimum.Vedtak
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

class KelvinClient(
    kelvinConfig: KelvinConfig
) {
    private val uri = kelvinConfig.proxyBaseUrl
    private val config = ClientConfig(scope = kelvinConfig.scope)

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun hentSakerByFnr(req: SakerRequest): List<SakStatus> {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = req
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create(uri).resolve("/api/datadeling/sakerByFnr"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw BehandlingsflytException("Feil ved forsøk på å hente sakByFnr fra Kelvin: ${e.message}")
        }
    }

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