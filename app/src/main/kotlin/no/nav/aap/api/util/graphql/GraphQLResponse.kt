package no.nav.aap.api.util.graphql

data class GraphQLResponse<Data>(
    val data: Data?,
    val errors: List<GraphQLError>?,
)
