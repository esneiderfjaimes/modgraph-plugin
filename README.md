# Mod Graph Gradle Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.github.esneiderfjaimes.modgraph?color=%2302303a)](https://plugins.gradle.org/plugin/io.github.esneiderfjaimes.modgraph)

A Gradle plugin that generates visual diagrams of module dependencies in multi-module projects.  
Currently supports output via the Graphviz `dot` command. Future support for other providers like Mermaid is planned.

---

## 🧩 Plugin ID

```kotlin
plugins {
    id("io.github.esneiderfjaimes.modgraph") version "0.0.1"
}
```

---

## 📦 What It Does

* Detects dependencies between modules in your Gradle project.
* Generates a diagram (SVG) of the module dependency graph.
* Saves the output to the `docs/graphs/` directory.
* Currently, requires a provider to be specified (e.g., `graphviz`).

---

## 📂 Directory Structure Example

```
your-project/
├── app/
│   └── build.gradle.kts
├── core/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── docs/
    └── graphs/
        └── module-dep.svg
```

---

## 🛠 How to Use

### 1. Run the Graph Generator

```bash
./gradlew generateModuleDependencyGraph --provider=graphviz
```

This will scan your modules and generate an SVG diagram at `docs/graphs/`.

---

### 2. Configuration (via Extension – optional for future versions)

The plugin includes a `modGraph` extension for future configuration, such as customizing the output directory.

```kotlin
modGraph {
    // Example (not yet implemented)
    // output.set(file("custom/path"))
}
```

---

## ✅ Tasks Provided

| Task Name                       | Description                                     |
|---------------------------------|-------------------------------------------------|
| `generateModuleDependencyGraph` | Generates an SVG diagram of module dependencies |

---

## 💡 Notes

* The `--provider` flag is required. The only supported value for now is:
    - `graphviz`: Requires [Graphviz](https://graphviz.org/) installed and accessible via the `dot` command in your terminal.

* Output is always saved to `docs/graphs/`.
* Only project module dependencies are considered (e.g., `implementation(project(":core"))`).

---

## 🔜 Planned Features

* Support for Mermaid diagrams.
* Configurable output path.
* Additional output formats (e.g., `.dot`, `.md`, etc.).

---