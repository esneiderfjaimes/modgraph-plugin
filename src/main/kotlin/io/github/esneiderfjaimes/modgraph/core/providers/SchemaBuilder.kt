package io.github.esneiderfjaimes.modgraph.core.providers

import io.github.esneiderfjaimes.modgraph.core.Module

interface SchemaBuilder {

    fun create(module: Module, directory: Map<String, Any>): String

}