package io.github.esneiderfjaimes.modgraph

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

@Suppress("unused")
class ModGraphPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create("modGraph", ModGraphExtension::class.java)
        tasks.register("generateModuleDependencyGraph", GenerateModGraphTask::class)
    }
}