package io.github.esneiderfjaimes.modgraph.core

import io.github.esneiderfjaimes.modgraph.GenerateModGraphTask.GraphProvider
import io.github.esneiderfjaimes.modgraph.core.providers.GraphvizBuilder
import io.github.esneiderfjaimes.modgraph.core.providers.MermaidBuilder

class GraphGenerator(val provider: ProjectProvider) {
    fun generate(moduleName: String, graphProvider: GraphProvider): String {
        val module = provider.getModuleByPath(moduleName)
        val paths = mutableListOf(module.path)
        paths += getAllDependenciesPaths(module)
        val map = transformPathsToDirectories(paths)
        return when (graphProvider) {
            GraphProvider.MERMAID -> MermaidBuilder().create(module, map)
            GraphProvider.GRAPHVIZ -> GraphvizBuilder().create(module, map)
        }
    }

    private fun getAllDependenciesPaths(module: Module): List<String> {
        val allDeps = mutableListOf<String>()
        allDeps.addAll(module.dependencies.map { it.path })
        module.dependencies.forEach { dep ->
            allDeps.addAll(getAllDependenciesPaths(dep))
        }
        return allDeps
    }

    private fun transformPathsToDirectories(paths: Collection<String>): Map<String, Any> {
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
                    current = current.getOrPut(part) {
                        mutableMapOf<String, Any>()
                    } as MutableMap<String, Any>
                }
            }
        }
        return tree
    }
}