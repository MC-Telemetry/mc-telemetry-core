package de.mctelemetry.core.neoforge.gametest

import de.mctelemetry.core.OTelCoreMod
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

@GameTestHolder(OTelCoreMod.MOD_ID)
@PrefixGameTestTemplate(false)
object GameLoadsTest {
    @JvmStatic
    @GameTest(templateNamespace = "mcotelcore", template="gametestempty")
    fun gameLoadsTest(helper: GameTestHelper) {
        helper.succeed()
    }
}
