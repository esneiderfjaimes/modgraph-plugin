package io.github.esneiderfjaimes.modgraph.core

import io.github.esneiderfjaimes.modgraph.core.providers.GraphvizBuilder
import io.github.esneiderfjaimes.modgraph.core.providers.MermaidBuilder

class GraphGenerator {
    fun generate(module: Module, engine: Engine, style: String?): String {
        val shemaBuilder = when (engine) {
            Engine.MERMAID -> MermaidBuilder()
            Engine.GRAPHVIZ -> GraphvizBuilder()
        }
        val paths = mutableListOf(module.path)
        paths += getAllDependenciesPaths(module)
        val node = transformPathsToNodes(module.path, paths)
        return shemaBuilder.create(module, node, style)
    }

    private fun getAllDependenciesPaths(module: Module): List<String> {
        val allDeps = mutableListOf<String>()
        allDeps.addAll(module.dependencies.map { it.path })
        module.dependencies.forEach { dep ->
            allDeps.addAll(getAllDependenciesPaths(dep))
        }
        return allDeps
    }

    private fun transformPathsToNodes(targetModulePath: String, paths: Collection<String>): Node {
        val root = Node()
        paths.forEach { path ->
            val parts = path.split(":")
            var current = root
            parts.forEachIndexed { index, part ->
                if (part.isEmpty()) return@forEachIndexed

                val isLast = index == parts.size - 1

                // Busca si ya existe un nodo hijo para 'part'
                val child = current.children[part]

                if (isLast) {
                    val isTargetModule = targetModulePath == path
                    // Último: módulo, crea o actualiza
                    if (child == null) {
                        current.children[part] = Node(path = path, isTarget = isTargetModule)
                    } else {
                        // Si existe y es directorio, deja children y actualiza path
                        current.children[part] = child.copy(path = path, isTarget = isTargetModule)
                    }
                } else {
                    // Intermedio: directorio
                    if (child == null) {
                        val newNode = Node()
                        current.children[part] = newNode
                        current = newNode
                    } else {
                        // Si existe, sigue descendiendo
                        current = child
                    }
                }
            }
        }

        return root
    }
}