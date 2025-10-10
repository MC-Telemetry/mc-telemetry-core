package de.mctelemetry.core.gametest

import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

// Classes containing @GameTest or @GameTestFactory need to be added to CommonGameTestFactory to be detected.
object MetricScrapeCommonTest {
    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun gameLoadsTest(helper: GameTestHelper) {
        val accessor = MetricsAccessor.INSTANCE
        helper.assertTrue(accessor != null, "Metrics accessor should not be null")
        val definitions = accessor!!.collect()
        helper.assertFalse(definitions.isEmpty(), "Collected metrics should not be empty")
        helper.succeed()
    }
}
