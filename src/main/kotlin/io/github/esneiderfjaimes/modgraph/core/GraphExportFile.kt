package io.github.esneiderfjaimes.modgraph.core

enum class GraphExportFile(val extension: String, val engine: Engine) {
    MERMAID("md", Engine.MERMAID),
    GRAPHVIZ("dot", Engine.GRAPHVIZ),
    SVG_GRAPHVIZ("svg", Engine.GRAPHVIZ),
    PNG_GRAPHVIZ("png", Engine.GRAPHVIZ),
    ;
}