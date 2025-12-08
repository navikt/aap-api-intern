package no.nav.aap.api.pdl

import no.nav.aap.api.util.graphql.GraphQLQueryException
import no.nav.aap.api.util.graphql.GraphQLRequest
import no.nav.aap.api.util.graphql.GraphQLResponse
import no.nav.aap.api.util.graphql.GraphQLResponseHandler
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(javaClass)

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
        return pdlIdenter.filter { ident -> ident.gruppe == "FOLKEREGISTERIDENT" }
    }

    private fun query(request: GraphQLRequest<PdlRequestVariables>): GraphQLResponse<PdlIdenterData> {
        val httpRequest = PostRequest(body = request)
        return try {
            requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
        } catch (e: GraphQLQueryException) {
            log.info("Feil ved oppslag mot PDL. Melding: ${e.message}. Kode: ${e.code}")
            if (e.code == "not_found") {
                GraphQLResponse(data = PdlIdenterData(PdlIdenter(emptyList())), errors = null)
            } else {
                throw e
            }
        }

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
