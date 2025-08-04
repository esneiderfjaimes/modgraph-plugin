package io.github.esneiderfjaimes.modgraph

import io.github.esneiderfjaimes.modgraph.core.Engine
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
	classDef Module color:#0D47A1,fill:#BBDEFB,stroke:#0D47A1
	classDef TargetModule stroke:#ff0000

	app_id@{shape: rounded, label: ":app"}
	app_id:::TargetModule

	subgraph core["core"]
		core__data_id@{shape: rounded, label: ":core:data"}
		core__data_id:::Module
		core__model_id@{shape: rounded, label: ":core:model"}
		core__model_id:::Module
		core__api_id@{shape: rounded, label: ":core:api"}
		core__api_id:::Module
		core__database_id@{shape: rounded, label: ":core:database"}
		core__database_id:::Module
	end

app_id --> core__data_id & core__model_id & core__api_id
core__data_id --> core__api_id & core__database_id & core__model_id
core__api_id --> core__model_id

linkStyle 0,1,2 stroke:#ff0000
""".trimIndent()
    val styleGRAPHVIZ = """
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
    val styleMERMAID = """
{
    "module": {
        "shape": "rounded",
        "fill":"#BBDEFB",
        "stroke":"#0D47A1",
        "color":"#0D47A1"
    },
    "targetModule": {
        "shape": "rounded",
        "stroke": "#ff0000"
    },
    "directLink": {
      "stroke": "#ff0000"
    }
}
""".trimIndent()

    @Test
    fun `should content syntax graphviz`() {
        val create = GraphGenerator(projectProvider)
            .generate(":app", Engine.GRAPHVIZ, styleGRAPHVIZ)
        assertEquals(rawGRAPHVIZ, create)
    }

    @Test
    fun `should content syntax mermaid`() {
        val create = GraphGenerator(projectProvider)
            .generate(":app", Engine.MERMAID, styleMERMAID)
        assertEquals(rawMERMAID, create)
    }
}
