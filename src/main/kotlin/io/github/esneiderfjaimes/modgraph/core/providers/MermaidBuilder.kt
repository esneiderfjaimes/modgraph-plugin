package io.github.esneiderfjaimes.modgraph.core.providers

import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.Node
import io.github.esneiderfjaimes.modgraph.core.StyleMap
import io.github.esneiderfjaimes.modgraph.core.normalizeId
import org.json.JSONObject

class MermaidBuilder : SchemaBuilder {

    override fun create(module: Module, node: Node, style: String?) = buildString {
        append(
            """
%%{ init: { 'flowchart': { 'curve': 'basis' } } }%%
graph TD
    """.trimIndent()
        )
        val moduleStyle = styleToMap(style)


        moduleStyle["module"]?.let {
            append("\n\tclassDef Module ")
            append(it)
        }
        moduleStyle["targetModule"]?.let {
            append("\n\tclassDef TargetModule ")
            append(it)
        }
        append("\n")

        graph(node, moduleStyle = moduleStyle)
        append("\n")

        val count = links(module)
        linksStyle(module, moduleStyle, count)
    }

    private fun StringBuilder.linksStyle(module: Module, moduleStyle: StyleMap, count: Int) {
        if (count > 0) {
            append("\n")
            val directModules = module.dependencies.size
            if (directModules > 1) {
                moduleStyle["directLink"]?.let { attributes ->
                    append("\nlinkStyle ")
                    // ids
                    0.until(directModules).joinToString(",").let { ids -> append(ids) }
                    append(" ")
                    append(attributes)
                }

                val otherModules = count - directModules
                if (otherModules > 0) {
                    moduleStyle["link"]?.let { attributes ->
                        append("\nlinkStyle ")
                        // ids
                        (directModules).until(count).joinToString(",").let { ids -> append(ids) }
                        append(" ")
                        append(attributes)
                    }
                }
            }
        }
    }

    private fun StringBuilder.graph(
        node: Node,
        moduleStyle: StyleMap,
        prevRoot: String = "",
        key: String = "",
        level: Int = 0
    ) {
        val containerStyle = moduleStyle["container"]

        val hasChildren = node.children.isNotEmpty()
        if (hasChildren && key.isNotEmpty()) {
            append("\n")
            append("\n")
            append("\t".repeat(level))
            append("subgraph ")
            append((prevRoot + key).normalizeId())
            append("[\"")
            append(key)
            append("\"]")

            containerStyle?.let {
                append("\n")
                append("\t".repeat(level + 1))
                append("style ")
                append(key.normalizeId())
                append(" ")
                append(it)
                append("\n")
            }
        }
        node.path?.let { path ->
            val level = if (hasChildren) level + 1 else level
            append("\n")
            append("\t".repeat(level))
            append(path.normalizeId())
            append("_id")
            append("@{")
            if (!node.isTarget) {
                moduleStyle["module_shape"]?.let { shape ->
                    append("shape: $shape, ")
                }
            } else {
                moduleStyle["targetModule_shape"]?.let { shape ->
                    append("shape: $shape, ")
                }
            }
            append("label: \"")
            append(path)
            append("\"")
            append("}")

            // style
            if (!node.isTarget) {
                append("\n")
                append("\t".repeat(level))
                append(path.normalizeId())
                append("_id")
                append(":::")
                append("Module")
            } else {
                append("\n")
                append("\t".repeat(level))
                append(path.normalizeId())
                append("_id")
                append(":::")
                append("TargetModule")
            }
        }

        node.children.forEach { (subKey, value) ->
            graph(
                node = value,
                moduleStyle = moduleStyle,
                prevRoot = prevRoot + key,
                key = subKey,
                level = level + 1
            )
        }

        if (hasChildren && key.isNotEmpty()) {
            append("\n")
            append("\t".repeat(level))
            append("end")
        }
    }

    private fun StringBuilder.links(
        module: Module,
        visited: MutableSet<String> = mutableSetOf()
    ): Int {
        var count = 0
        if (visited.contains(module.path)) {
            return 0
        }
        visited.add(module.path)

        if (module.dependencies.isEmpty()) {
            // skip modules with no dependencies
            return 0
        }

        append("\n")

        // module id 1
        append(module.path.normalizeId())
        append("_id")

        // arrow
        append(" --> ")

        val dependencies = module.dependencies.joinToString(" & ") { it.path.normalizeId() + "_id" }
        append(dependencies)

        count += module.dependencies.size

        module.dependencies.forEach { submodule ->
            count += links(submodule, visited)
        }

        return count
    }

    fun styleToMap(json: String?): StyleMap {
        if (json == null) {
            return emptyMap()
        }
        val map = mutableMapOf<String, String>()
        val root = JSONObject(json)

        fun especialAttribute(
            jsonKey: String,
            specialKey: String?,
        ) {
            if (!root.has(jsonKey)) return
            val props = root.getJSONObject(jsonKey)
            val keys = props.keys().asSequence().toMutableSet()
            if (keys.isEmpty()) {
                return
            }

            specialKey?.let {
                if (props.has(specialKey)) {
                    val shape = props.getString(specialKey)
                    map.put("${jsonKey}_$specialKey", shape)
                    props.remove(specialKey)
                    keys.remove(specialKey)
                }
            }

            val attributes = keys.joinToString(",") { key ->
                val value = props.get(key)
                val formatted = when (value) {
                    else -> value.toString()
                }
                "$key:$formatted"
            }
            map.put(jsonKey, attributes)
        }

        especialAttribute("container", null)
        especialAttribute("module", "shape")
        especialAttribute("targetModule", "shape")
        especialAttribute("link", "arrow")
        especialAttribute("directLink", "arrow")

        return map
    }
}