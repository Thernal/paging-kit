package io.thernal.pagingkit.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.thernal.pagingkit.detektrules.packageboundary.LayerPackageRequired
import org.junit.Assert.assertEquals
import org.junit.Test

class LayerPackageRequiredTest {
    private val rule = LayerPackageRequired(Config.empty)

    @Test
    fun `reports a file in the root package of an impl module`() {
        val findings = rule.lint(
            """
            package io.thernal.pagingkit.paging.impl

            class BackStackNavigator
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `reports a file in the root package of an api module`() {
        val findings = rule.lint(
            """
            package io.thernal.pagingkit.paging.api

            interface Navigator
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `reports a package that is not a layer`() {
        val findings = rule.lint(
            """
            package io.thernal.pagingkit.paging.api.deeplink

            class DeepLink
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `allows each layer package and its topical sub packages`() {
        val sources = listOf(
            "io.thernal.pagingkit.paging.api.domain",
            "io.thernal.pagingkit.paging.api.presentation.navigator",
            "io.thernal.pagingkit.paging.impl.data",
            "io.thernal.pagingkit.paging.impl.domain.deeplink",
            "io.thernal.pagingkit.paging.impl.presentation.scene",
        )

        sources.forEach { packageName ->
            assertEquals(0, rule.lint("package $packageName").size)
        }
    }

    @Test
    fun `ignores wiring build-logic and non-module packages`() {
        val sources = listOf(
            "io.thernal.pagingkit.paging.wiring",
            "io.thernal.pagingkit.buildlogic",
            "io.thernal.pagingkit.detektrules.style",
        )

        sources.forEach { packageName ->
            assertEquals(0, rule.lint("package $packageName").size)
        }
    }

    @Test
    fun `ignores a segment that only starts with a module name`() {
        val findings = rule.lint(
            """
            package io.thernal.pagingkit.implementation.detail

            class Detail
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }
}
