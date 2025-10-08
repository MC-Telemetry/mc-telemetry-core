package de.mctelemetry.core.fabric.gametest

import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class MetricScrapeTest : FabricGameTest {
    @GameTest(template="mcotelcore:gametestempty")
    fun collectMetricsTest(helper: GameTestHelper) {
        val accessor = MetricsAccessor.INSTANCE
        helper.assertTrue(accessor != null, "Metrics accessor should not be null")
        val definitions = accessor!!.collect()
        helper.assertFalse(definitions.isEmpty(), "Collected metrics should not be empty")
        helper.succeed()
    }
}
