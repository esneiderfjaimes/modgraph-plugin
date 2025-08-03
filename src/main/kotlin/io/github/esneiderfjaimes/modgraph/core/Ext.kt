package io.github.esneiderfjaimes.modgraph.core

fun String.normalizeId(): String {
    return removePrefix(":")
        .replace(":", "__")
        .replace("-", "___")
}

fun StringBuilder.appendId(string: String) {
    append("\"")
    append(string)
    append("\"")
}

fun String.normalizeId2(): String {
    return buildString {
        appendId(this@normalizeId2)
    }
}