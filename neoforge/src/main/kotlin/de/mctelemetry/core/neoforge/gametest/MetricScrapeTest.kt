package de.mctelemetry.core.neoforge.gametest

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

@GameTestHolder(OTelCoreMod.MOD_ID)
@PrefixGameTestTemplate(false)
object MetricScrapeTest {
    @JvmStatic
    @GameTest(templateNamespace = "mcotelcore", template="gametestempty")
    fun collectMetricsTest(helper: GameTestHelper) {
        val accessor = MetricsAccessor.INSTANCE
        helper.assertTrue(accessor != null, "Metrics accessor should not be null")
        val definitions = accessor!!.collect()
        helper.assertFalse(definitions.isEmpty(), "Collected metrics should not be empty")
        helper.succeed()
    }
}
