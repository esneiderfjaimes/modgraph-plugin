package io.github.esneiderfjaimes.modgraph

import io.github.esneiderfjaimes.modgraph.utils.id
import io.github.esneiderfjaimes.modgraph.utils.id2
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@Suppress("unused")
abstract class GenerateModGraphTask @Inject constructor(
    private val execOps: ExecOperations,
    objects: ObjectFactory
) : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Option(option = "provider", description = "graph svg provider")
    @get:Input
    abstract val provider: Property<String>

    @TaskAction
    fun generateSvgFiles() {
        val graphTypeName = provider.get()
        val graphProvider = GraphProvider.fromString(graphTypeName)

        // println("[OK] Generating ${graphProvider.extension} files")

        createGraph(project.rootProject, graphProvider)
        val outputDirFile = outputDir.get().asFile
        if (!outputDirFile.exists()) outputDirFile.mkdirs()

        val inputDirFile = File(outputDirFile, "temp-${graphProvider.extension}")
        inputDirFile.walkTopDown()
            .filter { it.isFile && it.extension == graphProvider.extension }
            .forEach { dotFile ->
                val outputFile = File(outputDirFile, dotFile.nameWithoutExtension + ".svg")
                // println("[OK] Generating: ${outputFile.name}")

                execOps.exec {
                    when (graphProvider) {
                        GraphProvider.MERMAID -> TODO()
                        GraphProvider.GRAPHVIZ -> {
                            commandLine(
                                "dot",
                                "-Tsvg",
                                dotFile.absolutePath,
                                "-o",
                                outputFile.absolutePath
                            )
                        }
                    }

                    println("[OK] Graph generated: ${outputFile.absolutePath}")
                }
            }

        if (inputDirFile.exists()) {
            inputDirFile.deleteRecursively()
        }
    }

    enum class GraphProvider(val extension: String) {
        MERMAID("md"),
        GRAPHVIZ("dot");

        companion object {
            fun fromString(value: String): GraphProvider {
                return values().find { it.name.lowercase() == value.lowercase() }!!
            }
        }
    }

    fun createGraph(project: Project, provider: GraphProvider) {
        try {
            val builder = when (provider) {
                GraphProvider.MERMAID -> MermaidBuilder()
                GraphProvider.GRAPHVIZ -> GraphvizBuilder()
            }
            val outputDir: File =
                project.rootProject.file("docs/graphs/temp-${provider.extension}")
            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
            outputDir.mkdirs()

            val files = mutableListOf<String>()
            project.rootProject.subprojects.forEach { subproject ->
                try {
                    // tree(subproject, { project.rootProject.subprojectByPath(it) })

                    val content = builder.create(subproject)
                    val outputDot = File(outputDir, "${subproject.id2}.${provider.extension}")
                    outputDot.writeText(content)

                    files.add(subproject.id2)

                    // println("[OK] file written to ${outputDot.absolutePath}")
                } catch (e: Exception) {
                    println("[!] ${subproject.id2} ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("[!] ${e.message}")
        }
    }

    fun Project.subprojectByPath(path: String): Project? {
        return rootProject.subprojects.find { it.path == path }
    }

    fun test(project: Project) {
        project.rootProject.subprojects.forEach { subproject ->
            println("Module: ${subproject.id}")
            val dependencies = mutableSetOf<String>()

            subproject.configurations.forEach { config ->
                config.dependencies.forEach { dep ->
                    if (dep is ProjectDependency) {
                        dependencies.add(dep.path)
                    }
                }
            }

            if (dependencies.isEmpty()) {
                println("  [!] No module dependencies.")
            } else {
                dependencies.forEach { path ->
                    println("${subproject.path}  -> $path")
                }
            }
            println()
        }
    }

    interface SchemaBuilder {
        fun create(project: Project): String
    }

    inner class MermaidBuilder : SchemaBuilder {
        override fun create(project: Project) = buildString {
            val tree = dependencePaths(project, { project.subprojectByPath(it) })
            val map = pathsToMap(tree)
            append(
                "%%{ init: { 'flowchart': { 'curve': 'basis' } } }%%" +
                        "\ngraph TD"
            )
            graph("", map)
            links(project.rootProject.subprojects)
        }

        private fun StringBuilder.graph(key: String, any: Any, level: Int = 0) {
            when (any) {
                is String -> {
                    append("\n")
                    append("\t".repeat(level))
                    append(any.replace(":", "_"))
                    append("_id")
                    append("(")
                    append(any)
                    append(")")
                }

                is Map<*, *> -> {
                    if (key.isNotEmpty()) {
                        append("\n")
                        append("\t".repeat(level))
                        append("subgraph $key")
                    }
                    @Suppress("UNCHECKED_CAST")
                    val map = any as Map<String, Any>
                    map.forEach { (key, value) ->
                        graph(key, value, level + 1)
                    }
                    if (key.isNotEmpty()) {
                        append("\n")
                        append("end")
                    }
                }
            }
        }

        private fun StringBuilder.links(subprojects: Collection<Project>) {
            subprojects.forEach { subproject ->
                val dependencies = mutableSetOf<String>()

                subproject.configurations.forEach { config ->
                    config.dependencies.forEach { dep ->
                        if (dep is ProjectDependency) {
                            dependencies.add(dep.path)
                        }
                    }
                }

                if (dependencies.isNotEmpty()) {
                    dependencies.forEach { path ->
                        if (path == subproject.path) {
                            return@forEach
                        }
                        append("\n")
                        append("\t".repeat(1))
                        append(subproject.path.replace(":", "_"))
                        append("_id")
                        append(" --> ")
                        append(path.replace(":", "_"))
                        append("_id")
                    }
                }
            }
        }
    }

    inner class GraphvizBuilder : SchemaBuilder {
        override fun create(project: Project): String {
            return buildString {
                val tree = dependencePaths(project, { project.subprojectByPath(it) })
                val map = pathsToMap(tree)
                append(
                    """digraph unix {
    rankdir=TB; // top to bottom
	fontname="Helvetica,Arial,sans-serif"
	node [fontname="Helvetica,Arial,sans-serif"]
	edge [fontname="Helvetica,Arial,sans-serif"]
	node [color=lightblue2, style=filled];
"""
                )
                if (map.isEmpty()) {
                    throw IllegalStateException("Empty map")
                }
                graph("", map)
                append("\n")
                links(project, subprojectProvider = { project.subprojectByPath(it) })
                append("\n}")
            }
        }

        private fun StringBuilder.graph(key: String, any: Any, level: Int = 0) {
            when (any) {
                is String -> {
                    append("\n")
                    append("\t".repeat(level))
                    append(any.replace(":", "_"))
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
                        append("subgraph cluster_$key {")
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
                        graph(key, value, level + 1)
                    }
                    if (key.isNotEmpty()) {
                        append("\n")
                        append("\t".repeat(level))
                        append("}")
                    }
                }
            }
        }

        /*        private fun StringBuilder.links(subprojects: Collection<Project>) {
                    subprojects.forEach { subproject ->
                        val dependencies = mutableSetOf<String>()

                        subproject.configurations.forEach { config ->
                            config.dependencies.forEach { dep ->
                                if (dep is ProjectDependency) {
                                    dependencies.add(dep.id)
                                }
                            }
                        }

                        if (dependencies.isNotEmpty()) {
                            dependencies.forEach { path ->
                                if (path == subproject.id) {
                                    return@forEach
                                }
                                append("\n")
                                append("\t".repeat(1))
                                append(subproject.id.replace(":", "_"))
                                append("_id")
                                append(" -> ")
                                append(path.replace(":", "_"))
                                append("_id")
                                append(";")
                            }
                        }
                    }
                }

                private fun StringBuilder.links(subproject: Project) {
                    val dependencies = mutableSetOf<String>()

                    subproject.configurations.forEach { config ->
                        config.dependencies.forEach { dep ->
                            if (dep is ProjectDependency) {
                                dependencies.add(dep.id)
                            }
                        }
                    }

                    if (dependencies.isNotEmpty()) {
                        dependencies.forEach { path ->
                            if (path == subproject.id) {
                                return@forEach
                            }
                            append("\n")
                            append("\t".repeat(1))
                            append(subproject.id.replace(":", "_"))
                            append("_id")
                            append(" -> ")
                            append(path.replace(":", "_"))
                            append("_id")
                            append(";")
                        }
                    }
                }*/

        fun StringBuilder.links(project: Project, subprojectProvider: (String) -> Project?) {
            val dependencies = mutableSetOf<String>()
            project.configurations.forEach { config ->
                config.dependencies.forEach { dep ->
                    if (dep is ProjectDependency) {
                        dependencies.add(dep.id)
                    }
                }
            }

            if (dependencies.isNotEmpty()) {
                dependencies.forEach { path ->
                    if (path == project.id) {
                        return@forEach
                    }
                    append("\n")
                    append("\t".repeat(1))
                    append(project.id.replace(":", "_"))
                    append("_id")
                    append(" -> ")
                    append(path.replace(":", "_"))
                    append("_id")
                    append(";")
                    val subproject = subprojectProvider(path) ?: error("Could not find $path")
                    links(subproject, subprojectProvider)
                }
            }
        }
    }

    fun tree(project: Project, subprojectProvider: (String) -> Project?, level: Int = 0) {
        println("${"\t".repeat(level)}${project.path}")

        val dependencies = mutableSetOf<String>()
        project.configurations.forEach { config ->
            config.dependencies.forEach { dep ->
                if (dep is ProjectDependency) {
                    dependencies.add(dep.id)
                }
            }
        }

        if (dependencies.isNotEmpty()) {
            dependencies.forEach { path ->
                if (path == project.id) {
                    return@forEach
                }

                val subproject = subprojectProvider(path) ?: error("Could not find $path")
                tree(subproject, subprojectProvider, level + 1)
            }
        }
    }

    fun dependencePaths(
        project: Project,
        subprojectProvider: (String) -> Project?,
        level: Int = 0
    ): MutableSet<String> {
        val dependencies = mutableSetOf<String>()
        project.configurations.forEach { config ->
            config.dependencies.forEach { dep ->
                if (dep is ProjectDependency) {
                    dependencies.add(dep.id)
                }
            }
        }

        val newTree = mutableSetOf<String>()
        if (dependencies.isNotEmpty()) {
            dependencies.forEach { path ->
                if (path == project.id) {
                    return@forEach
                }

                val subproject = subprojectProvider(path) ?: error("Could not find $path")
                newTree += dependencePaths(subproject, subprojectProvider, level + 1)
            }
        }

        dependencies.addAll(newTree)

        return dependencies
    }

    fun pathsToMap(paths: Collection<String>): MutableMap<String, Any> {
        val tree = mutableMapOf<String, Any>()
        paths.forEach { path ->
            val parts = path.split(":")
            var current = tree
            parts.forEachIndexed { index, part ->
                if (part.isEmpty()) {
                    return@forEachIndexed
                }

                if (index == parts.size - 1) {
                    current[part] = path
                } else {
                    @Suppress("UNCHECKED_CAST")
                    current =
                        current.getOrPut(part) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
                }
            }
        }
        return tree
    }
}
