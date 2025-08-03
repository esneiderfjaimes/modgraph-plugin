package io.github.esneiderfjaimes.modgraph.core

data class Node(
    val path: String? = null,
    val isTarget: Boolean = false,
    val children: MutableMap<String, Node> = mutableMapOf()
)