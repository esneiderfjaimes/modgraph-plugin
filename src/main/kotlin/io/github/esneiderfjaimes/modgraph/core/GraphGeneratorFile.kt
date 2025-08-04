package io.github.esneiderfjaimes.modgraph.core

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import org.gradle.api.Project
import java.io.File

interface GraphGeneratorFile {

    fun toFile(
        content: String,
        outputDir: File,
        project: Project,
        graphExportFile: GraphExportFile,
    ): File

}

object GraphGeneratorFileImpl : GraphGeneratorFile {

    override fun toFile(
        content: String,
        outputDir: File,
        project: Project,
        graphExportFile: GraphExportFile,
    ): File {
        val path = project.path.normalizeId()
        val file = File(outputDir, "${path}.${graphExportFile.extension}")
        when (graphExportFile) {
            GraphExportFile.MERMAID -> {
                file.writeText(buildString {
                    append("```mermaid\n")
                    append(content)
                    append("\n```\n")
                })
            }

            GraphExportFile.GRAPHVIZ -> {
                file.writeText(content)
            }

            GraphExportFile.SVG_GRAPHVIZ -> {
                Graphviz.fromString(content)
                    .render(Format.SVG)
                    .toFile(file)
            }
        }
        return file
    }

}