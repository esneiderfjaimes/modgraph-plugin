package io.github.esneiderfjaimes.modgraph.core

fun String.normalizeId(): String {
    return removePrefix(":")
        .replace(":", "__")
        .replace("-", "___")
}
