package de.mctelemetry.core.fabric.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class GameLoadsTest : FabricGameTest {
    @GameTest(template="mcotelcore:gametestempty")
    fun gameLoadsTest(helper: GameTestHelper) {
        helper.succeed()
    }
}
