package io.github.esneiderfjaimes.modgraph

import io.github.esneiderfjaimes.modgraph.GenerateModGraphTask.GraphProvider
import io.github.esneiderfjaimes.modgraph.core.GraphGenerator
import io.github.esneiderfjaimes.modgraph.core.Module
import io.github.esneiderfjaimes.modgraph.core.ProjectProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphGeneratorTest {

    val moduleCoreCore = Module(":core:core:uwu", listOf())
    val moduleCore = Module(":test", listOf(moduleCoreCore))
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
digraph {
	graph [color="lightgrey", style="dashed", fontname="Helvetica,Arial,sans-serif"];
	node [fillcolor="lightblue", style="filled", fontname="Helvetica,Arial,sans-serif"];

	":app" [color="red"]

	subgraph cluster_core {
		label = "core"
		tooltip = "core"

		":core:data"
		":core:model"
		":core:api"
		":core:database"
	}

	":app" -> {":core:data" ":core:model" ":core:api"} [color="red"]
	":core:data" -> {":core:api" ":core:database" ":core:model"}
	":core:api" -> {":core:model"}
}""".trimIndent()

    val rawMERMAID = """
%%{ init: { 'flowchart': { 'curve': 'basis' } } }%%
graph TD
	app_id(:app)

	subgraph core
		core__data_id(:core:data)
		core__model_id(:core:model)
		core__api_id(:core:api)
		core__database_id(:core:database)
	end

app_id --> core__data_id & core__model_id & core__api_id
core__data_id --> core__api_id & core__database_id & core__model_id
core__api_id --> core__model_id
""".trimIndent()
    val style = """
{
    "module": {
        "fontname": "Helvetica,Arial,sans-serif",
        "style": "filled",
        "fillcolor": "lightblue"
    },
    "container": {
        "fontname": "Helvetica,Arial,sans-serif",
        "color" : "lightgrey",
		"style" : "dashed"
    },
    "targetModule": {
      "color": "red"
    },
    "directLink": {
      "color": "red"
    }
}
    """.trimIndent()

    @Test
    fun `should content syntax graphviz`() {
        val create = GraphGenerator(projectProvider)
            .generate(":app", GraphProvider.GRAPHVIZ, style)
        assertEquals(rawGRAPHVIZ, create)
    }

    @Test
    fun `should content syntax mermaid`() {
        val create = GraphGenerator(projectProvider)
            .generate(":app", GraphProvider.MERMAID, null)
        assertEquals(rawMERMAID, create)
    }
}
