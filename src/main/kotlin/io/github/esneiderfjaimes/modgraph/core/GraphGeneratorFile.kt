package io.github.esneiderfjaimes.modgraph.core

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import org.gradle.api.Project
import java.io.File

interface GraphGeneratorFile {

    fun write(
        file: File,
        content: String,
        project: Project,
        graphExportFile: GraphExportFile,
    )

}

object GraphGeneratorFileImpl : GraphGeneratorFile {

    override fun write(
        file: File,
        content: String,
        project: Project,
        graphExportFile: GraphExportFile,
    ) {
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

            GraphExportFile.PNG_GRAPHVIZ -> {
                Graphviz.fromString(content)
                    .render(Format.PNG)
                    .toFile(file)
            }
        }
    }

}