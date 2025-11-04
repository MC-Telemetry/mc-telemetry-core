package de.mctelemetry.core.gametest.tests

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

// Classes containing @GameTest or @GameTestFactory need to be added to CommonGameTestFactory to be detected.
object GameLoadsCommonTest {
    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun gameLoadsTest(helper: GameTestHelper) {
        helper.succeed()
    }
}
