package api.util.graphql

data class GraphQLRequest<Variables>(
    val query: String,
    val variables: Variables,
)
