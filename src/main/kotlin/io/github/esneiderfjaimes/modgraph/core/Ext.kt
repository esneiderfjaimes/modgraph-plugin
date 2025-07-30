package io.github.esneiderfjaimes.modgraph.core

fun String.normalizeId(): String {
    return replace(":", "_").replace("-", "_").removePrefix("_")
}

const val SHOW_LOG = false
const val SHOW_DANGER_LOG = false