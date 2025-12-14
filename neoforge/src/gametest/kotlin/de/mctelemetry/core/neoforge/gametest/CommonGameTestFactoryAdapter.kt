package de.mctelemetry.core.neoforge.gametest

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.gametest.CommonGameTestFactory
import net.minecraft.gametest.framework.GameTestGenerator
import net.minecraft.gametest.framework.TestFunction
import net.neoforged.neoforge.gametest.GameTestHolder

@Suppress("unused")
@GameTestHolder(OTelCoreMod.MOD_ID)
class CommonGameTestFactoryAdapter {
    //class with companion object because neoforge apparently has trouble with top-level object declarations
    companion object {

        @GameTestGenerator
        @JvmStatic
        fun testGenerator(): Collection<TestFunction> {
            return CommonGameTestFactory.generateCommonGameTests()
        }
    }
}
