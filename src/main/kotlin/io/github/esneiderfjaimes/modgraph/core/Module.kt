package io.github.esneiderfjaimes.modgraph.core

data class Module(val path: String, val dependencies: List<Module>)
