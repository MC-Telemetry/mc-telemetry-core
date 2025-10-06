package de.mctelemetry.core.forge.gametest

import de.mctelemetry.core.OTelCoreMod
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(OTelCoreMod.MOD_ID)
@PrefixGameTestTemplate(false)
object GameLoadsTest {
    @JvmStatic
    @GameTest(templateNamespace = "mcotelcore", template="gametestempty")
    fun gameLoadsTest(helper: GameTestHelper) {
        helper.succeed()
    }
}
