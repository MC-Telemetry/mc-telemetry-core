package de.mctelemetry.core.gametest.tests.observations.scraper.redstone

import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.items.OTelCoreModItems
import de.mctelemetry.core.gametest.utils.assertBlockEntityDataC
import de.mctelemetry.core.gametest.utils.assertBlockStateC
import de.mctelemetry.core.gametest.utils.assertFalseC
import de.mctelemetry.core.gametest.utils.assertNotNullC
import de.mctelemetry.core.gametest.utils.assertNullC
import de.mctelemetry.core.gametest.utils.assertValueEqualC
import de.mctelemetry.core.gametest.utils.thenExecuteForC
import de.mctelemetry.core.gametest.utils.thenWaitUntilC
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

// Classes containing @GameTest or @GameTestFactory need to be added to CommonGameTestFactory to be detected.
object RedstoneScraperBlockTest {

    private val BasePos = BlockPos(0, 1, 0)

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun canSetBlockTest(helper: GameTestHelper) {
        helper.setBlock(BasePos, OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get())
        helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun canPlaceBlockTest(helper: GameTestHelper) {
        helper.destroyBlock(BasePos)
        val localNorth = helper.testRotation.rotate(Direction.NORTH)
        val player = helper.makeMockPlayer(GameType.DEFAULT_MODE).also {
            it.moveTo(helper.absoluteVec(Vec3(0.5, 1.0, 0.5)))
        }
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK))
            helper.placeAt(
                player,
                ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK),
                BasePos.relative(localNorth, -1),
                localNorth
            )
            helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
            helper.succeed()
        } finally {
            player.discard()
        }
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun canSetAndRemoveBlockTest(helper: GameTestHelper) {
        helper.setBlock(BasePos, OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get())
        helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
        helper.setBlock(BasePos, Blocks.AIR)
        helper.assertBlockPresent(Blocks.AIR, BasePos)
        helper.succeed()
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun canPlaceAndDestroyBlockTest(helper: GameTestHelper) {
        helper.destroyBlock(BasePos)
        val localNorth = helper.testRotation.rotate(Direction.NORTH)
        val player = helper.makeMockPlayer(GameType.DEFAULT_MODE).also {
            it.moveTo(helper.absoluteVec(Vec3(0.5, 1.0, 0.5)))
        }
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK))
            helper.placeAt(
                player,
                ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK),
                BasePos.relative(localNorth, -1),
                localNorth
            )
            helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
            helper.level.destroyBlock(helper.absolutePos(BasePos), true, player)
            helper.assertItemEntityCountIs(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK.get(), BasePos, 1.0, 1)
            helper.assertBlockPresent(Blocks.AIR, BasePos)
            helper.succeed()
        } finally {
            player.discard()
        }
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun canSetAndRemoveBlockAfterDelayTest(helper: GameTestHelper) {
        helper.setBlock(BasePos, OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get())
        helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
        helper.runAfterDelay(5) {
            helper.setBlock(BasePos, Blocks.AIR)
            helper.assertBlockPresent(Blocks.AIR, BasePos)
            helper.succeed()
        }
    }

    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun canPlaceAndDestroyBlockAfterDelayTest(helper: GameTestHelper) {
        helper.destroyBlock(BasePos)
        val localNorth = helper.testRotation.rotate(Direction.NORTH)
        val player = helper.makeMockPlayer(GameType.DEFAULT_MODE).also {
            it.moveTo(helper.absoluteVec(Vec3(0.5, 1.0, 0.5)))
        }
        runWithExceptionCleanup(cleanup = player::discard) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK))
            helper.placeAt(
                player,
                ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK),
                BasePos.relative(localNorth, -1),
                localNorth
            )
            helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
            helper.runAfterDelay(5) {
                try {
                    helper.level.destroyBlock(helper.absolutePos(BasePos), true, player)
                    helper.assertItemEntityCountIs(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK.get(), BasePos, 2.0, 1)
                    helper.assertBlockPresent(Blocks.AIR, BasePos)
                    helper.succeed()
                } finally {
                    player.discard()
                }
            }
        }
    }


    @Suppress("unused")
    @JvmStatic
    @GameTest
    fun newTransitionsToDefaultWarningTest(helper: GameTestHelper) {
        helper.setBlock(BasePos, OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get())
        helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
        helper.onEachTick {
            helper.assertBlockPresent(OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(), BasePos)
        }
        helper.assertBlockStateC(BasePos, { "Expected to start in error state" }) {
            it.getValue(ObservationSourceContainerBlock.ERROR) == ObservationSourceErrorState.Type.Errors
        }
        fun validation() { // shared validation logic between "wait until this is true" and "assert this remains true"
            helper.assertBlockEntityDataC<ObservationSourceContainerBlockEntity>(
                BasePos,
                { "Expected to be in a warning state" }) {
                val states = helper.assertNotNullC(it.observationStatesIfInitialized, "observationStates")
                helper.assertFalseC(states.isEmpty(), "Expected observationStates to not be empty")
                states.forEach { (source, state) ->
                    helper.assertValueEqualC(
                        state.errorState,
                        ObservationSourceErrorState.NotConfigured,
                        "errorState of $source"
                    )
                    helper.assertNullC(state.configuration, "configuration of $source")
                }
                ObservationSourceErrorState.Type.Warnings == it.blockState.getValue(ObservationSourceContainerBlock.ERROR)
            }
        }
        helper.startSequence()
            .thenWaitUntilC(::validation) // wait until validation succeeds
            .thenExecuteForC(5, ::validation) // assert validation continues to succeed for 5 ticks
            .thenSucceed() // succeed
        helper.runAfterDelay(2, ::validation) // assert validation succeeds within 2 ticks
    }
}
