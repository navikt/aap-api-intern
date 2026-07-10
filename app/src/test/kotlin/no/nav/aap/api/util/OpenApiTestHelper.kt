package no.nav.aap.api.util

import com.fasterxml.jackson.databind.JsonNode

object OpenApiTestHelper {

    fun finnPropertyNavnForTag(openapi: JsonNode, tag: String): Map<String, List<String>> {
        val schemas = openapi["components"]["schemas"] ?: return emptyMap()

        val alleRefs = finnSchemaRefsMedTag(openapi, tag)
            .fold(mutableSetOf<String>()) { visited, ref ->
                samleAlleRefs(ref, schemas, visited)
                visited
            }

        return alleRefs
            .mapNotNull { schemaName ->
                val properties = schemas[schemaName]?.get("properties") ?: return@mapNotNull null
                schemaName to properties.properties().map { it.key }
            }
            .toMap()
    }

    private fun finnSchemaRefsMedTag(openapi: JsonNode, tag: String): Set<String> {
        val paths = openapi["paths"] ?: return emptySet()
        val refs = mutableSetOf<String>()

        for ((_, pathItem) in paths.properties()) {
            for ((_, operation) in pathItem.properties()) {
                if (!operation.isObject) continue
                val tags = operation["tags"] ?: continue
                if (tags.any { it.asText() == tag }) {
                    finnRefs(operation, refs)
                }
            }
        }
        return refs
    }

    private fun samleAlleRefs(schemaName: String, schemas: JsonNode, visited: MutableSet<String>) {
        if (!visited.add(schemaName)) return
        val schema = schemas[schemaName] ?: return
        val childRefs = mutableSetOf<String>()
        finnRefs(schema, childRefs)
        childRefs.forEach { samleAlleRefs(it, schemas, visited) }
    }

    private fun finnRefs(node: JsonNode, refs: MutableSet<String>) {
        when {
            node.isObject -> {
                node[$$"$ref"]
                    ?.takeIf { it.isTextual }
                    ?.let { refs.add(it.asText().removePrefix("#/components/schemas/")) }
                node.properties().forEach { (_, child) -> finnRefs(child, refs) }
            }
            node.isArray -> node.forEach { finnRefs(it, refs) }
        }
    }
}
