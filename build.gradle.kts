import org.jetbrains.intellij.platform.gradle.TestFrameworkType

// Build script for the Antigravity CLI Opener IntelliJ Platform plugin.
// Docs: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

plugins {
    // Kotlin/JVM — plugin sources are written in Kotlin.
    id("org.jetbrains.kotlin.jvm")
    // Maintains CHANGELOG.md in the "Keep a Changelog" format.
    id("org.jetbrains.changelog")
    // The IntelliJ Platform Gradle Plugin (v2) – pulls in the IDE SDK, runs sandbox,
    // verifies the plugin, and publishes to JetBrains Marketplace.
    id("org.jetbrains.intellij.platform")
}

dependencies {
    // JUnit 4 for any unit tests that get added later.
    testImplementation(libs.junit)

    // IntelliJ Platform-specific dependencies. Read more:
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // Compile against this IntelliJ IDEA build. Any newer JetBrains IDE on the
        // same platform branch will be binary-compatible at runtime.
        intellijIdea("2025.3.5")

        // The bundled Terminal plugin ships the `TerminalToolWindowManager` API that
        // our action uses to spawn a shell tab and run `agr`. Without this entry the
        // import in `OpenAntigravityCliAction.kt` will not resolve.
        bundledPlugin("org.jetbrains.plugins.terminal")

        // Test framework wiring for `runIde`/`test` Gradle tasks.
        testFramework(TestFrameworkType.Platform)
    }
}
