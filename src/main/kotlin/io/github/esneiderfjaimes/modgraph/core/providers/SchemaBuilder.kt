package io.github.esneiderfjaimes.modgraph.core.providers

import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.Node

interface SchemaBuilder {

    fun create(module: Module, node: Node, style: String?): String

}