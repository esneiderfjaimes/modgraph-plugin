package io.github.esneiderfjaimes.modgraph.core.providers

import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.Node
import io.github.esneiderfjaimes.modgraph.core.StyleMap
import io.github.esneiderfjaimes.modgraph.core.appendId
import io.github.esneiderfjaimes.modgraph.core.normalizeId
import io.github.esneiderfjaimes.modgraph.core.normalizeId2
import org.json.JSONObject

class GraphvizBuilder : SchemaBuilder {

    override fun create(
        module: Module,
        node: Node,
        style: String?
    ) = buildString {
        append("digraph {\n")
        val moduleStyle = styleToMap(style)
        style(moduleStyle)
        graph(node, style = moduleStyle["targetModule"])
        append("\n")
        links(module, moduleStyle = moduleStyle)
        append("\n}")
    }

    fun StringBuilder.style(style: Map<String, String>) {
        if (style.isEmpty()) {
            return
        }
        SECTIONS_RELATIONSHIPS.forEach { (section, relationship) ->
            val attributes = style[section] ?: return@forEach
            appendLine("\t$relationship [$attributes];")
        }
    }

    private fun StringBuilder.graph(
        node: Node,
        style: String? = null,
        key: String = "",
        level: Int = 0
    ) {
        val hasChildren = node.children.isNotEmpty()
        if (hasChildren && key.isNotEmpty()) {
            append("\n")
            append("\n")
            append("\t".repeat(level))
            append("subgraph cluster_${key.normalizeId()} {")
            append("\n")
            // label
            append("\t".repeat(level + 1))
            append("label = ")
            appendId(key)
            append("\n")
            // tooltip
            append("\t".repeat(level + 1))
            append("tooltip = ")
            appendId(key)
            append("\n")
        }

        node.path?.let { path ->
            val level = if (hasChildren) level + 1 else level
            append("\n")
            append("\t".repeat(level))
            appendId(path)
            // style
            if (node.isTarget) {
                style?.let { append(" [$it]") }
            }
        }

        node.children.forEach { (key, value) ->
            graph(
                node = value,
                style = style,
                key = key,
                level = level + 1
            )
        }

        if (hasChildren && key.isNotEmpty()) {
            append("\n")
            append("\t".repeat(level))
            append("}")
        }
    }

    private fun StringBuilder.links(
        module: Module,
        moduleStyle: Map<String, String>? = null,
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
        appendId(module.path)

        // arrow
        append(" -> ")
        append("{")

        val dependencies = module.dependencies.joinToString(" ") { it.path.normalizeId2() }
        append(dependencies)

        append("}")

        if (first) {
            val directLink = moduleStyle?.get("directLink")
            if (directLink != null) {
                append(" [${directLink}]")
            }
        }

        module.dependencies.forEach { submodule ->
            links(
                module = submodule,
                moduleStyle = null,
                visited = visited,
                first = false
            )
        }
    }

    fun styleToMap(json: String?): StyleMap {
        if (json == null) {
            return emptyMap()
        }
        val root = JSONObject(json)
        return SECTIONS_WHITELIST.mapNotNull { section ->
            if (root.has(section)) {
                val props = root.getJSONObject(section)
                val attributes = props.keys().asSequence()
                    .map { key ->
                        val value = props.get(key)
                        val formatted = when (value) {
                            is Number, is Boolean -> value.toString()
                            else -> "\"$value\""
                        }
                        "$key=$formatted"
                    }
                    .joinToString(", ")
                section to attributes
            } else {
                null
            }
        }.toMap()
    }

    companion object {
        val SECTIONS_WHITELIST = listOf("container", "module", "link", "directLink", "targetModule")

        private val SECTIONS_RELATIONSHIPS = mapOf(
            "container" to "graph",
            "module" to "node",
            "link" to "edge"
        )
    }
}
