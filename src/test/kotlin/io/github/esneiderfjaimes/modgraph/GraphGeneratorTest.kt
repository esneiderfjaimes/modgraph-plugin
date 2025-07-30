package io.github.esneiderfjaimes.modgraph

import io.github.esneiderfjaimes.modgraph.GenerateModGraphTask.GraphProvider
import io.github.esneiderfjaimes.modgraph.core.GraphGenerator
import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.ProjectProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphGeneratorTest {

    val moduleModel = Module(":core:model", listOf())
    val moduleDatabase = Module(":core:database", listOf())
    val moduleApi = Module(":core:api", listOf(moduleModel))
    val moduleData = Module(":core:data", listOf(moduleApi, moduleDatabase, moduleModel))
    val moduleApp = Module(":app", listOf(moduleData, moduleModel, moduleApi))

    val projectProvider = object : ProjectProvider {
        override fun getModuleByPath(path: String): Module {
            return when (path) {
                ":core:database" -> moduleDatabase
                ":core:api" -> moduleApi
                ":core:model" -> moduleModel
                ":core:data" -> moduleData
                ":app" -> moduleApp
                else -> throw IllegalArgumentException("Unknown path: $path")
            }
        }
    }

    val rawGRAPHVIZ = """
digraph unix {
    rankdir=TB;
    fontname="Helvetica,Arial,sans-serif"
    node [fontname="Helvetica,Arial,sans-serif", color=lightblue2, style=filled];

	app_id [label=":app"];

	subgraph cluster_core {
		label = "core";
		color = lightgrey;
		style = dashed;

		core_data_id [label=":core:data"];
		core_model_id [label=":core:model"];
		core_api_id [label=":core:api"];
		core_database_id [label=":core:database"];
	}

	app_id -> {core_data_id core_model_id core_api_id} [color=red];
	core_data_id -> {core_api_id core_database_id core_model_id};
	core_api_id -> {core_model_id};
}""".trimIndent()

    val rawMERMAID = """
%%{ init: { 'flowchart': { 'curve': 'basis' } } }%%
graph TD
	app_id(:app)

	subgraph core
		core_data_id(:core:data)
		core_model_id(:core:model)
		core_api_id(:core:api)
		core_database_id(:core:database)
	end

app_id --> core_data_id & core_model_id & core_api_id
core_data_id --> core_api_id & core_database_id & core_model_id
core_api_id --> core_model_id
""".trimIndent()
    @Test
    fun `should content syntax graphviz`() {
        val create = GraphGenerator(projectProvider).generate(":app", GraphProvider.GRAPHVIZ)
        assertEquals(rawGRAPHVIZ, create)
    }

    @Test
    fun `should content syntax mermaid`() {
        val create = GraphGenerator(projectProvider).generate(":app", GraphProvider.MERMAID)
        assertEquals(rawMERMAID, create)
    }
}
