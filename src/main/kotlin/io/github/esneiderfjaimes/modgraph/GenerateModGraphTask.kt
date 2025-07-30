package io.github.esneiderfjaimes.modgraph

import io.github.esneiderfjaimes.modgraph.core.GraphGenerator
import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.ProjectProvider
import io.github.esneiderfjaimes.modgraph.core.SHOW_DANGER_LOG
import io.github.esneiderfjaimes.modgraph.core.SHOW_LOG
import io.github.esneiderfjaimes.modgraph.core.normalizeId
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
) : DefaultTask(), ProjectProvider {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Option(option = "provider", description = "graph svg provider")
    @get:Input
    abstract val provider: Property<String>

    private val graphGenerator = GraphGenerator(this)

    @TaskAction
    fun generateSvgFiles() {
        val graphTypeName = provider.get()
        val graphProvider = GraphProvider.fromString(graphTypeName)

        // println("[OK] Generating ${graphProvider.extension} files")

        generateGraphs(graphProvider)
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

    fun generateGraphs(provider: GraphProvider) {
        try {
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

                    val path = subproject.path.normalizeId()
                    val content = graphGenerator.generate(subproject.path, provider)
                    val outputDot = File(outputDir, "${path}.${provider.extension}")
                    outputDot.writeText(content)

                    files.add(path)

                    // println("[OK] file written to ${outputDot.absolutePath}")
                } catch (e: Exception) {
                    println("[!] ${subproject.path} ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("[!] ${e.message}")
        }
    }

    override fun getModuleByPath(path: String): Module {
        return moduleByPath(path)
    }

    private val _subprojectDir = mutableMapOf<String, Project>()

    fun subprojectByPath(path: String): Project {
        return _subprojectDir.getOrPut(path) {
            project.rootProject.subprojects.find { it.path == path }
                ?: throw GradleException("Could not find $path")
        }
    }

    private val _directDependenciesDir = mutableMapOf<String, Set<String>>()

    fun directDependenciesByPath(project: Project): Set<String> {
        val key = project.path
        val fromCache = _directDependenciesDir[key]
        if (fromCache != null) {
            if (SHOW_DANGER_LOG) {
                println("[directDependenciesByPath] Using cache for $key")
            }
            return fromCache
        }
        val dependencies = mutableSetOf<String>()
        project.configurations.forEach { config ->
            config.dependencies.forEach dependencies@ { dep ->
                if (dep is ProjectDependency) {
                    // skip self
                    if (key == dep.path) {
                        return@dependencies
                    }

                    dependencies.add(dep.path)
                }
            }
        }
        if (SHOW_LOG) {
            println("[directDependenciesByPath] $key -> $dependencies")
        }
        _directDependenciesDir[key] = dependencies
        return dependencies
    }

    private val _modules = mutableMapOf<String, Module>()

    private fun moduleByPath(
        path: String,
        visited: MutableSet<String> = mutableSetOf()
    ): Module {
        _modules[path]?.let { return it }

        if (!visited.add(path)) {
            return Module(path, emptyList())
        }

        val subproject = subprojectByPath(path)
        val deps = directDependenciesByPath(subproject).map { depPath ->
            moduleByPath(depPath, visited)
        }

        val module = Module(path, deps)
        _modules[path] = module
        return module
    }
}
