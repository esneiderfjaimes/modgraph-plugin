package io.github.esneiderfjaimes.modgraph.core.providers

import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.normalizeId

class GraphvizBuilder : SchemaBuilder {

    override fun create(module: Module, directory: Map<String, Any>) = buildString {
        append(
            """digraph unix {
    rankdir=TB;
    fontname="Helvetica,Arial,sans-serif"
    node [fontname="Helvetica,Arial,sans-serif", color=lightblue2, style=filled];
"""
        )
        graph(directory)
        append("\n")
        //  links(module.path, module.dependencies)
        links(module)
        append("\n}")
    }

    private fun StringBuilder.graph(any: Any, key: String = "", level: Int = 0) {
        when (any) {
            is String -> {
                append("\n")
                append("\t".repeat(level))
                append(any.normalizeId())
                append("_id")
                append(" [label=\"")
                append(any)
                append("\"];")
            }

            is Map<*, *> -> {
                if (key.isNotEmpty()) {
                    append("\n")
                    append("\n")
                    append("\t".repeat(level))
                    append("subgraph cluster_${key.normalizeId()} {")
                    append("\n")
                    append("\t".repeat(level + 1))
                    append("label = \"")
                    append(key)
                    append("\";")
                    append("\n")
                    append("\t".repeat(level + 1))
                    append("color = lightgrey;")
                    append("\n")
                    append("\t".repeat(level + 1))
                    append("style = dashed;")
                    append("\n")
                }
                @Suppress("UNCHECKED_CAST")
                val map = any as Map<String, Any>
                map.forEach { (key, value) ->
                    graph(value, key, level + 1)
                }
                if (key.isNotEmpty()) {
                    append("\n")
                    append("\t".repeat(level))
                    append("}")
                }
            }
        }
    }

    private fun StringBuilder.links(
        path: String,
        modules: List<Module>,
        visited: MutableSet<String> = mutableSetOf(),
        first: Boolean = true
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
            append(" -> ")

            // module id 2
            append(module.path.normalizeId())
            append("_id")

            if (first) {
                append(" [color=red]")
            }
            append(";")

            links(module.path, module.dependencies, visited, false)
        }
    }

    private fun StringBuilder.links(
        module: Module,
        visited: MutableSet<String> = mutableSetOf(),
        first: Boolean = true
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
        append("\t")

        // module id 1
        append(module.path.normalizeId())
        append("_id")

        // arrow
        append(" -> ")
        append("{")

        val dependencies = module.dependencies.joinToString(" ") { it.path.normalizeId() + "_id" }
        append(dependencies)

        append("}")

        if (first) {
            append(" [color=red]")
        }
        append(";")

        module.dependencies.forEach { submodule ->
            links(submodule, visited, false)
        }
    }
}