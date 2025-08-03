plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.esneiderfjaimes"
version = "0.0.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("guru.nidi:graphviz-java:0.18.1")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.1")
    implementation("org.json:json:20250517")
}

gradlePlugin {
    website = "https://github.com/esneiderfjaimes/modgraph-plugin"
    vcsUrl = "https://github.com/esneiderfjaimes/modgraph-plugin.git"
    plugins {
        create("modgraphPlugin") {
            id = "io.github.esneiderfjaimes.modgraph"
            implementationClass = "io.github.esneiderfjaimes.modgraph.ModGraphPlugin"
            displayName = "ModGraph - Visualize Module Dependencies"
            description =
                "A Gradle plugin for generating visual diagrams of module dependencies using pluggable rendering backends."
            tags = listOf(
                "graph",
                "dependencies",
                "modules",
                "visualization",
                "architecture"
            )
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}