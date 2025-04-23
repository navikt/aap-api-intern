package api.pdl

import api.util.graphql.GraphQLResponse
import api.util.graphql.GraphQLResponseHandler
import api.util.graphql.GraphQLRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

interface IPdlClient {
    fun hentAlleIdenterForPerson(personIdent: String): List<PdlIdent>
}

class PdlClient : IPdlClient {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.pdl.url"))
    private val config =
        ClientConfig(
            scope = requiredConfigForKey("integrasjon.pdl.scope"),
            additionalHeaders = listOf(Header("Behandlingsnummer", "B287")),
        )

    private val client =
        RestClient(
            config = config,
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = GraphQLResponseHandler(),
        )

    override fun hentAlleIdenterForPerson(personIdent: String): List<PdlIdent> {
        val request = GraphQLRequest(IDENT_QUERY, variables = PdlRequestVariables(personIdent))
        val response = query(request)
        val pdlIdenter =
            checkNotNull(response.data?.hentIdenter?.identer) {
                "Fant ingen identer i PDL for person"
            }
        return pdlIdenter.filter { it.gruppe == "FOLKEREGISTERIDENT" }
    }

    private fun query(request: GraphQLRequest<PdlRequestVariables>): GraphQLResponse<PdlIdenterData> {
        val httpRequest = PostRequest(body = request)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }
}

private const val ident = "\$ident"
val IDENT_QUERY =
    """
    query($ident: ID!) {
        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
    """.trimIndent()
