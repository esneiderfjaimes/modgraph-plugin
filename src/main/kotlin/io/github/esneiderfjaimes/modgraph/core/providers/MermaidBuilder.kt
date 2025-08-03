package io.github.esneiderfjaimes.modgraph.core.providers

import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.Node
import io.github.esneiderfjaimes.modgraph.core.normalizeId

class MermaidBuilder : SchemaBuilder {

    override fun create(module: Module, node: Node, style: String?) = buildString {
        append(
            """
%%{ init: { 'flowchart': { 'curve': 'basis' } } }%%
graph TD
    """.trimIndent()
        )
       // graph(directory)
        append("\n")
        links(module)
        // links(module.path, module.dependencies)
    }

    private fun StringBuilder.graph(any: Any, key: String = "", level: Int = 0) {
        when (any) {
            is String -> {
                append("\n")
                append("\t".repeat(level))
                append(any.normalizeId())
                append("_id")
                append("(")
                append(any)
                append(")")
            }

            is Map<*, *> -> {
                if (key.isNotEmpty()) {
                    append("\n")
                    append("\n")
                    append("\t".repeat(level))
                    append("subgraph $key")
                }
                @Suppress("UNCHECKED_CAST")
                val map = any as Map<String, Any>
                map.forEach { (key, value) ->
                    graph(value, key, level + 1)
                }
                if (key.isNotEmpty()) {
                    append("\n")
                    append("\t".repeat(level))
                    append("end")
                }
            }
        }
    }

    private fun StringBuilder.links(
        path: String,
        modules: List<Module>,
        visited: MutableSet<String> = mutableSetOf()
    ) {
        if (visited.contains(path)) {
            return
        }
        visited.add(path)
        modules.forEach { module ->
            append("\n")
            append("\t")

            // module id 1
            append(path.normalizeId())
            append("_id")

            // arrow
            append(" --> ")

            // module id 2
            append(module.path.normalizeId())
            append("_id")

            links(module.path, module.dependencies, visited)
        }
    }

    private fun StringBuilder.links(
        module: Module,
        visited: MutableSet<String> = mutableSetOf()
    ) {
        if (visited.contains(module.path)) {
            return
        }
        visited.add(module.path)

        if (module.dependencies.isEmpty()) {
            // skip modules with no dependencies
            return
        }

        append("\n")

        // module id 1
        append(module.path.normalizeId())
        append("_id")

        // arrow
        append(" --> ")

        val dependencies = module.dependencies.joinToString(" & ") { it.path.normalizeId() + "_id" }
        append(dependencies)

        module.dependencies.forEach { submodule ->
            links(submodule, visited)
        }
    }
}