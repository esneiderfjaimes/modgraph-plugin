package io.github.esneiderfjaimes.modgraph.core

interface ProjectProvider {

    fun getModuleByPath(path: String): Module

}
