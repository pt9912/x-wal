package com.xwal

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies hexagonal architecture constraints at build-file level.
 * Ensures the hexagon (core, ports, application) remains framework-free.
 */
class ArchitectureTest {

    private val projectRoot = findProjectRoot()

    private fun findProjectRoot(): File {
        var dir = File(System.getProperty("user.dir"))
        while (!File(dir, "settings.gradle.kts").exists() && dir.parentFile != null) {
            dir = dir.parentFile
        }
        return dir
    }

    private fun readBuildFile(modulePath: String): String {
        val file = File(projectRoot, "$modulePath/build.gradle.kts")
        return if (file.exists()) file.readText() else ""
    }

    // ========== Hexagon Framework-Free Checks ==========
    // Check dependency declarations only (ignore comments)

    private fun extractDependencies(buildContent: String): String =
        buildContent.lines()
            .filter { it.trimStart().startsWith("implementation") || it.trimStart().startsWith("api(") || it.trimStart().startsWith("ksp(") }
            .joinToString("\n")

    @Test
    fun `hexagon core has no Micronaut dependency`() {
        val deps = extractDependencies(readBuildFile("hexagon/core"))
        assertFalse(deps.contains("micronaut", ignoreCase = true),
            "hexagon/core must not depend on Micronaut. Found: $deps")
    }

    @Test
    fun `hexagon ports has no Micronaut dependency`() {
        val deps = extractDependencies(readBuildFile("hexagon/ports"))
        assertFalse(deps.contains("micronaut", ignoreCase = true),
            "hexagon/ports must not depend on Micronaut. Found: $deps")
    }

    @Test
    fun `hexagon application has no Micronaut dependency`() {
        val deps = extractDependencies(readBuildFile("hexagon/application"))
        assertFalse(deps.contains("micronaut", ignoreCase = true),
            "hexagon/application must not depend on Micronaut. Found: $deps")
    }

    // ========== Dependency Direction Checks ==========

    @Test
    fun `hexagon core depends on nothing internal`() {
        val build = readBuildFile("hexagon/core")
        assertFalse(build.contains("project("), "hexagon/core must not depend on any project module")
    }

    @Test
    fun `hexagon ports depends only on hexagon core`() {
        val build = readBuildFile("hexagon/ports")
        assertTrue(build.contains(":hexagon:core"), "hexagon/ports must depend on hexagon:core")
        assertFalse(build.contains(":hexagon:application"), "hexagon/ports must not depend on hexagon:application")
        assertFalse(build.contains(":adapters:"), "hexagon/ports must not depend on any adapter")
        assertFalse(build.contains(":app"), "hexagon/ports must not depend on app")
    }

    @Test
    fun `hexagon application depends only on hexagon ports and core`() {
        val build = readBuildFile("hexagon/application")
        assertTrue(build.contains(":hexagon:ports"), "hexagon/application must depend on hexagon:ports")
        assertFalse(build.contains(":adapters:"), "hexagon/application must not depend on any adapter")
        assertFalse(build.contains(":app"), "hexagon/application must not depend on app")
    }

    @Test
    fun `driving web adapter depends on hexagon ports not application`() {
        val build = readBuildFile("adapters/driving/web")
        assertTrue(build.contains(":hexagon:ports"), "driving/web must depend on hexagon:ports")
        assertFalse(build.contains(":hexagon:application"), "driving/web must not depend on hexagon:application")
    }

    @Test
    fun `driven persistence depends on hexagon ports not application`() {
        val build = readBuildFile("adapters/driven/persistence")
        assertTrue(build.contains(":hexagon:ports"), "driven/persistence must depend on hexagon:ports")
        assertFalse(build.contains(":hexagon:application"), "driven/persistence must not depend on hexagon:application")
    }

    @Test
    fun `driven engine depends on hexagon ports not application`() {
        val build = readBuildFile("adapters/driven/engine")
        assertTrue(build.contains(":hexagon:ports"), "driven/engine must depend on hexagon:ports")
        assertFalse(build.contains(":hexagon:application"), "driven/engine must not depend on hexagon:application")
    }

    // ========== No Adapter Cross-Dependencies ==========

    @Test
    fun `driven persistence does not depend on other adapters`() {
        val build = readBuildFile("adapters/driven/persistence")
        assertFalse(build.contains(":adapters:driven:engine"), "persistence must not depend on engine")
        assertFalse(build.contains(":adapters:driven:identity"), "persistence must not depend on identity")
        assertFalse(build.contains(":adapters:driven:observability"), "persistence must not depend on observability")
        assertFalse(build.contains(":adapters:driving:"), "persistence must not depend on driving adapters")
    }

    @Test
    fun `driven engine does not depend on other adapters`() {
        val build = readBuildFile("adapters/driven/engine")
        assertFalse(build.contains(":adapters:driven:persistence"), "engine must not depend on persistence")
        assertFalse(build.contains(":adapters:driven:identity"), "engine must not depend on identity")
        assertFalse(build.contains(":adapters:driven:observability"), "engine must not depend on observability")
        assertFalse(build.contains(":adapters:driving:"), "engine must not depend on driving adapters")
    }

    // ========== Module Structure Checks ==========

    @Test
    fun `all 10 modules exist`() {
        val modules = listOf(
            "hexagon/core", "hexagon/ports", "hexagon/application",
            "adapters/driving/web", "adapters/driving/cli",
            "adapters/driven/persistence", "adapters/driven/engine",
            "adapters/driven/identity", "adapters/driven/observability",
            "app"
        )
        for (module in modules) {
            assertTrue(File(projectRoot, "$module/build.gradle.kts").exists(),
                "Module $module must have a build.gradle.kts")
        }
    }

    @Test
    fun `app module depends on all other modules`() {
        val build = readBuildFile("app")
        assertTrue(build.contains(":hexagon:core"))
        assertTrue(build.contains(":hexagon:ports"))
        assertTrue(build.contains(":hexagon:application"))
        assertTrue(build.contains(":adapters:driving:web"))
        assertTrue(build.contains(":adapters:driven:persistence"))
        assertTrue(build.contains(":adapters:driven:engine"))
        assertTrue(build.contains(":adapters:driven:identity"))
        assertTrue(build.contains(":adapters:driven:observability"))
    }
}
