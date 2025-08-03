package io.github.esneiderfjaimes.modgraph

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModGraphPluginTest {

    private lateinit var testProjectDir: File

    @BeforeAll
    fun setup() {
        testProjectDir = createTempDir(prefix = "modgraph-test")

        // Crea settings.gradle.kts con dos módulos
        File(testProjectDir, "settings.gradle.kts").writeText(
            """
        rootProject.name = "test-project"
        include(":app", ":core")
        """.trimIndent()
        )

        // style.json
        val styleFile = File(testProjectDir, "style.json")
        styleFile.writeText(
            """
{
  "graph": {
    "fontname": "Helvetica,Arial,sans-serif"
  },
  "module": {
    "shape": "box",
    "style": "filled,rounded",
    "fillcolor": "lightblue",
    "fontname": "Helvetica,Arial,sans-serif"
  },
  "link": {
    "color": "gray"
  },
  "targetModule": {
    "color": "red"
  },
  "directLink": {
    "color": "red"
  }
}
        """.trimIndent()
        )
        val normalizedPath = styleFile.absolutePath.replace(File.separatorChar, '/')

        // build.gradle.kts del root (con el plugin aplicado)
        File(testProjectDir, "build.gradle.kts").writeText(
            """
plugins {
    id("io.github.esneiderfjaimes.modgraph")
}
modGraph {
   stylePath = "$normalizedPath"
}
        """.trimIndent()
        )

        // build.gradle.kts de :core (módulo sin dependencias)
        File(testProjectDir, "core").mkdir()
        File(testProjectDir, "core/build.gradle.kts").writeText(
            """
    plugins {
        `java-library`
    }

        """.trimIndent()
        )

        // build.gradle.kts de :app (depende de :core)
        File(testProjectDir, "app").mkdir()
        File(testProjectDir, "app/build.gradle.kts").writeText(
            """
    plugins {
        `java-library`
    }

    dependencies {
        implementation(project(":core"))
    }
    """.trimIndent()
        )

    }

    @Test
    fun `should generate svg file with module dependencies`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("generateModuleDependencyGraph", "--stacktrace")
            .forwardOutput()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModuleDependencyGraph")?.outcome)

        val graphsDir = File(testProjectDir, "docs/graphs")
        assertTrue(
            graphsDir.exists(),
            "Expected docs/graphs directory to exist, ${graphsDir.absolutePath}"
        )

        val svgFiles = graphsDir.listFiles { _, name -> name.endsWith(".svg") } ?: emptyArray()
        assertTrue(svgFiles.isNotEmpty(), "Expected at least one .svg file in docs/graphs")

        val svgContent = svgFiles.first().readText()
        assertTrue(svgContent.contains("<svg"), "Expected SVG content")
    }

    @Test
    fun `should generate svg file with module dependencies custom output`() {
        val randomNumber = Random.nextInt(1000)
        val graphsDir = File(testProjectDir, "graphs-${randomNumber}")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments(
                "generateModuleDependencyGraph",
                "--output",
                graphsDir.absolutePath,
                "--stacktrace"
            )
            .forwardOutput()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModuleDependencyGraph")?.outcome)

        assertTrue(
            graphsDir.exists(),
            "Expected random directory to exist, ${graphsDir.absolutePath}"
        )

        val svgFiles = graphsDir.listFiles { _, name -> name.endsWith(".svg") } ?: emptyArray()
        assertTrue(svgFiles.isNotEmpty(), "Expected at least one .svg file in random directory")

        val svgContent = svgFiles.first().readText()
        assertTrue(svgContent.contains("<svg"), "Expected SVG content")
    }
}