package io.github.esneiderfjaimes.modgraph.core

import org.json.JSONObject

fun String.normalizeId(): String {
    return removePrefix(":")
        .replace(":", "__")
        .replace("-", "___")
}

fun String.normalizeFile(): String {
    return removePrefix(":")
        .replace("_", "__")
        .replace(":", "_")
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

fun Node.toJSONObject(): JSONObject = JSONObject().apply {
    put("path", path)
    put("isTarget", isTarget)
    val childrenJSON = JSONObject()
    children.forEach { (key, value) -> childrenJSON.put(key, value.toJSONObject()) }
    put("children", childrenJSON)
}