package io.github.esneiderfjaimes.modgraph

import io.github.esneiderfjaimes.modgraph.core.GraphGenerator
import io.github.esneiderfjaimes.modgraph.core.GraphGeneratorFile
import io.github.esneiderfjaimes.modgraph.core.GraphGeneratorFileImpl
import io.github.esneiderfjaimes.modgraph.core.GraphExportFile
import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.ProjectProvider
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
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

    @get:Option(option = "output", description = "Output directory")
    @get:Input
    abstract val outputDirPath: Property<String>

    @get:Option(option = "module", description = "Optional module name")
    @get:Input
    @get:Optional
    abstract val moduleName: Property<String>

    @get:Option(option = "style", description = "Style file")
    @get:Input
    @get:Optional
    abstract val stylePath: Property<String>

    private val extension: ModGraphExtension
        get() = project.extensions.getByType(ModGraphExtension::class.java)

    init {
        outputDirPath.convention(
            // provided by extension
            extension.outputDirPath.orNull
            // default
                ?: File(project.rootProject.projectDir, "docs/graphs").absolutePath
        )

        extension.stylePath.orNull?.let {
            stylePath.convention(it)
        }

        moduleName.convention(null as String?)
    }

    // resolve output dir
    private val resolvedOutputDir: File
        get() {
            val rawPath = outputDirPath.get()
            val file = File(rawPath)
            val outputDir = if (file.isAbsolute) file
            else File(project.rootProject.projectDir, rawPath)
            outputDir.mkdirs()
            return outputDir
        }

    private val graphGenerator = GraphGenerator(this)

    private val graphGeneratorFile: GraphGeneratorFile = GraphGeneratorFileImpl

    private val resolvedGraphExportFile: GraphExportFile
        get() {
            /*
                val graphTypeName = provider.get()
                val graphProvider = GraphProvider.fromString(graphTypeName)
            */
            return GraphExportFile.SVG_GRAPHVIZ
        }

    @TaskAction
    fun generateSvgFiles() {
        try {
            val moduleName = moduleName.orNull

            // target module name is not provided
            if (moduleName != null) {
                require(moduleName.startsWith(":")) {
                    "Invalid module path '${moduleName}'. It must start with ':' (e.g. :app, :lib-core)"
                }

                val project = subprojectByPath(moduleName)
                generateModuleDependencyGraph(project)
            } else {
                // Project is root project
                if (project == project.rootProject) {
                    // generate all module dependency graph
                    project.rootProject.subprojects.forEach { subproject ->
                        generateModuleDependencyGraph(subproject)
                    }
                    return
                }

                // Project is not root project
                generateModuleDependencyGraph(project)
            }
        } catch (e: Exception) {
            logger.error("[modgraph] export failed.", e)
        }
    }

    private fun readStyleFile(): String? {
        val file = File(stylePath.orNull ?: return null)
        return file.readText()
    }

    private fun generateModuleDependencyGraph(
        project: Project,
    ) {
        try {
            val outputDir: File = resolvedOutputDir
            val graphExportFile: GraphExportFile = resolvedGraphExportFile
            val style = readStyleFile()

            // tree(subproject, { project.rootProject.subprojectByPath(it) })

            // generate content
            val content = graphGenerator.generate(
                moduleName = project.path,
                engine = graphExportFile.engine,
                style = style
            )

            // export to file
            val file = graphGeneratorFile.toFile(
                content = content,
                outputDir = outputDir,
                project = project,
                graphExportFile = graphExportFile,
            )

            val normalizedPath = file.absolutePath.replace(File.separatorChar, '/')
            logger.lifecycle("[modgraph] module $path exported to file:///${normalizedPath}.")
        } catch (e: Exception) {
            logger.error("[modgraph] module ${project.path} export failed.", e)
        }
    }

    override fun getModuleByPath(path: String): Module {
        return moduleByPath(path)
    }

    private val _subprojectDir = mutableMapOf<String, Project>()

    fun subprojectByPath(path: String): Project {
        return _subprojectDir.getOrPut(path) {
            project.rootProject.subprojects.find { it.path == path }
                ?: throw GradleException(
                    "Could not find module '$path'. Available modules:\n" +
                            project.rootProject.subprojects.joinToString("\n") { it.path }
                )
        }
    }

    private val _directDependenciesDir = mutableMapOf<String, Set<String>>()

    fun directDependenciesByPath(project: Project): Set<String> {
        val key = project.path
        val fromCache = _directDependenciesDir[key]
        if (fromCache != null) {
            logger.debug("[directDependenciesByPath] Using cache for $key")
            return fromCache
        }
        val dependencies = mutableSetOf<String>()
        project.configurations.forEach { config ->
            config.dependencies.forEach dependencies@{ dep ->
                if (dep is ProjectDependency) {
                    // skip self
                    if (key == dep.path) {
                        return@dependencies
                    }

                    dependencies.add(dep.path)
                }
            }
        }
        logger.info("[directDependenciesByPath] {} -> {}", key, dependencies)
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
