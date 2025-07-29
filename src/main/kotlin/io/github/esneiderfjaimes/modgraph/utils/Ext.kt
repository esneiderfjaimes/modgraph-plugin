package io.github.esneiderfjaimes.modgraph.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

val Project.id: String get() = path//.replace(":", "_").removePrefix("_")
val Project.id2: String get() = path.replace(":", "_").removePrefix("_")
val ProjectDependency.id: String get() = path//.replace(":", "_").removePrefix("_")
