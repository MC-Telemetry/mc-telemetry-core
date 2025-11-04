package de.mctelemetry.core.gametest.tests.observations.scraper.redstone

import de.mctelemetry.core.gametest.utils.assertValueEqualC
import de.mctelemetry.core.gametest.utils.getBlockEntityC
import de.mctelemetry.core.utils.doubleInstrument
import de.mctelemetry.core.gametest.utils.observation.InstrumentGameTestHelper.Companion.instruments
import de.mctelemetry.core.gametest.utils.observation.withConfiguredStartupSequence
import de.mctelemetry.core.gametest.utils.thenExecuteAfterC
import de.mctelemetry.core.gametest.utils.thenExecuteC
import de.mctelemetry.core.utils.longInstrument
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import kotlin.math.floor

// Classes containing @GameTest or @GameTestFactory need to be added to CommonGameTestFactory to be detected.
object RedstoneScraperBlockChestTest {

    private val ChestBlockPos: BlockPos = BlockPos(0, 1, 1)
    private val fillItem: Item = Items.EGG

    private inline fun <T, R : Any> assertStateModificationSequence(
        helper: GameTestHelper,
        sequence: GameTestSequence,
        params: Sequence<T>,
        crossinline modificationBlock: (T) -> R,
        crossinline assertionBlock: (T, R) -> Unit,
    ): GameTestSequence {
        return params.fold(sequence) { subSequence, param ->
            var modificationResult: R? = null
            subSequence.thenExecuteC {
                if (!helper.testInfo.hasFailed()) {
                    modificationResult = modificationBlock(param)
                }
            }.thenExecuteAfterC(1) {
                assertionBlock(param, modificationResult!!)
                modificationResult = null
            }
        }
    }

    object Free {
        object Undirected {

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.chest", timeoutTicks = 500)
            fun longScrapeChestPowerTest(helper: GameTestHelper) {
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                helper.withConfiguredStartupSequence { sequence, instruments ->
                    val directPowerInstrument by lazy { instruments.getValue("direct_power").longInstrument }
                    val powerInstrument by lazy { instruments.getValue("power").longInstrument }
                    val sharedAttributes = Attributes.of(
                        AttributeKey.stringArrayKey("pos"),
                        helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                    )
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { _, _ ->
                            with(helper.instruments) {
                                directPowerInstrument.assertRecordsSingle(sharedAttributes, 0L)
                                powerInstrument.assertRecordsSingle(sharedAttributes, 0L)
                            }
                        }).thenSucceed()
                }
            }

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.chest", timeoutTicks = 500)
            fun longScrapeChestComparatorTest(helper: GameTestHelper) {
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                helper.withConfiguredStartupSequence { sequence, instruments ->
                    val comparatorInstrument by lazy { instruments.getValue("comparator").longInstrument }
                    val sharedAttributes = Attributes.of(
                        AttributeKey.stringArrayKey("pos"),
                        helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                    )
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = sequenceOf(0 to 0) + (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { _, _ ->
                            with(helper.instruments) {
                                val analogBaseValue: Int =
                                    chest.blockState.getAnalogOutputSignal(
                                        helper.level,
                                        helper.absolutePos(ChestBlockPos)
                                    )
                                comparatorInstrument.assertRecordsSingle(sharedAttributes, analogBaseValue.toLong())
                            }
                        }).thenSucceed()
                }
            }

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.chest", timeoutTicks = 500)
            fun doubleScrapeChestComparatorTest(helper: GameTestHelper) {
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                val chestContainer = ChestBlock.getContainer(
                    chest.blockState.block as ChestBlock,
                    chest.blockState,
                    helper.level,
                    helper.absolutePos(ChestBlockPos),
                    false
                )
                helper.withConfiguredStartupSequence(useDouble = { true }) { sequence, instruments ->
                    val comparatorInstrument by lazy { instruments.getValue("comparator").doubleInstrument }
                    val sharedAttributes = Attributes.of(
                        AttributeKey.stringArrayKey("pos"),
                        helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                    )
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = sequenceOf(0 to 0) + (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { (slot, count), _ ->
                            with(helper.instruments) {
                                val analogBaseValue: Int =
                                    chest.blockState.getAnalogOutputSignal(
                                        helper.level,
                                        helper.absolutePos(ChestBlockPos)
                                    )
                                if (chestContainer?.isEmpty == false) {
                                    val calculatedValue =
                                        1.0 + 14 * (slot.toDouble() +
                                                count.toDouble() / fillItem.defaultMaxStackSize
                                                ) / chestContainer.containerSize
                                    comparatorInstrument.assertRecordsSingle(
                                        sharedAttributes,
                                        calculatedValue
                                    )
                                    helper.assertValueEqualC(
                                        floor(calculatedValue).toInt(),
                                        analogBaseValue,
                                        "floored advanced value for ($slot, $count)"
                                    )
                                } else {
                                    comparatorInstrument.assertRecordsSingle(sharedAttributes, 0.0)
                                }
                            }
                        }).thenSucceed()
                }
            }
        }

        object Directed {

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.chest.directed", timeoutTicks = 500)
            fun longScrapeChestPowerTest(helper: GameTestHelper) {
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                helper.withConfiguredStartupSequence { sequence, instruments ->
                    val directPowerInstrument by lazy { instruments.getValue("direct_power").longInstrument }
                    val powerInstrument by lazy { instruments.getValue("power").longInstrument }
                    val sharedAttributes: Map<Direction, Attributes> = Direction.entries.associateWith { direction ->
                        Attributes.of(
                            AttributeKey.stringArrayKey("pos"),
                            helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                            AttributeKey.stringKey("dir"),
                            helper.instruments.formatDirection(direction),
                        )
                    }
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { _, _ ->
                            with(helper.instruments) {
                                directPowerInstrument.assertRecords {
                                    for (attributes in sharedAttributes.values) {
                                        assertRecordsLong(attributes, 0L)
                                    }
                                }
                                powerInstrument.assertRecords {
                                    for (attributes in sharedAttributes.values) {
                                        assertRecordsLong(attributes, 0L)
                                    }
                                }
                            }
                        }).thenSucceed()
                }
            }
        }
    }

    object Blocked {
        object Undirected {

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.air", timeoutTicks = 500)
            fun longScrapeChestPowerTest(helper: GameTestHelper) {
                helper.setBlock(ChestBlockPos, Blocks.CHEST)
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                helper.withConfiguredStartupSequence { sequence, instruments ->
                    val directPowerInstrument by lazy { instruments.getValue("direct_power").longInstrument }
                    val powerInstrument by lazy { instruments.getValue("power").longInstrument }
                    val sharedAttributes = Attributes.of(
                        AttributeKey.stringArrayKey("pos"),
                        helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                    )
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { _, _ ->
                            with(helper.instruments) {
                                directPowerInstrument.assertRecordsSingle(sharedAttributes, 0L)
                                powerInstrument.assertRecordsSingle(sharedAttributes, 0L)
                            }
                        }).thenSucceed()
                }
            }

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.air", timeoutTicks = 500)
            fun longScrapeChestComparatorTest(helper: GameTestHelper) {
                helper.setBlock(ChestBlockPos, Blocks.CHEST)
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                helper.withConfiguredStartupSequence { sequence, instruments ->
                    val comparatorInstrument by lazy { instruments.getValue("comparator").longInstrument }
                    val sharedAttributes = Attributes.of(
                        AttributeKey.stringArrayKey("pos"),
                        helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                    )
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = sequenceOf(0 to 0) + (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { _, _ ->
                            with(helper.instruments) {
                                val analogBaseValue: Int =
                                    chest.blockState.getAnalogOutputSignal(
                                        helper.level,
                                        helper.absolutePos(ChestBlockPos)
                                    )
                                comparatorInstrument.assertRecordsSingle(sharedAttributes, analogBaseValue.toLong())
                            }
                        }).thenSucceed()
                }
            }

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.air", timeoutTicks = 500)
            fun doubleScrapeChestComparatorTest(helper: GameTestHelper) {
                helper.setBlock(ChestBlockPos, Blocks.CHEST)
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                val chestContainer = ChestBlock.getContainer(
                    chest.blockState.block as ChestBlock,
                    chest.blockState,
                    helper.level,
                    helper.absolutePos(ChestBlockPos),
                    false
                )
                helper.withConfiguredStartupSequence(useDouble = { true }) { sequence, instruments ->
                    val comparatorInstrument by lazy { instruments.getValue("comparator").doubleInstrument }
                    val sharedAttributes = Attributes.of(
                        AttributeKey.stringArrayKey("pos"),
                        helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                    )
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = sequenceOf(0 to 0) + (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { (slot, count), _ ->
                            with(helper.instruments) {
                                val analogBaseValue: Int =
                                    chest.blockState.getAnalogOutputSignal(
                                        helper.level,
                                        helper.absolutePos(ChestBlockPos)
                                    )
                                if (chestContainer?.isEmpty == false) {
                                    val calculatedValue =
                                        1.0 + 14 * (
                                                (slot.toDouble() / chestContainer.containerSize) +
                                                        (count.toDouble() / fillItem.defaultMaxStackSize)
                                                )
                                    comparatorInstrument.assertRecordsSingle(
                                        sharedAttributes,
                                        calculatedValue
                                    )
                                    helper.assertValueEqualC(
                                        floor(calculatedValue).toInt(),
                                        analogBaseValue,
                                        "floored advanced value for ($slot, $count)"
                                    )
                                } else {
                                    comparatorInstrument.assertRecordsSingle(sharedAttributes, 0.0)
                                }
                            }
                        }).thenSucceed()
                }
            }
        }

        object Directed {

            @Suppress("unused")
            @JvmStatic
            @GameTest(template = "mcotelcore:redstonescraper.air.directed", timeoutTicks = 500)
            fun longScrapeChestPowerTest(helper: GameTestHelper) {
                helper.setBlock(ChestBlockPos, Blocks.CHEST)
                val chest = helper.getBlockEntityC<ChestBlockEntity>(ChestBlockPos)
                helper.withConfiguredStartupSequence { sequence, instruments ->
                    val directPowerInstrument by lazy { instruments.getValue("direct_power").longInstrument }
                    val powerInstrument by lazy { instruments.getValue("power").longInstrument }
                    val sharedAttributes: Map<Direction, Attributes> = Direction.entries.associateWith { direction ->
                        Attributes.of(
                            AttributeKey.stringArrayKey("pos"),
                            helper.instruments.formatGlobalBlockPos(ChestBlockPos),
                            AttributeKey.stringKey("dir"),
                            helper.instruments.formatDirection(direction),
                        )
                    }
                    assertStateModificationSequence(
                        helper,
                        sequence,
                        params = (0 until chest.containerSize).asSequence().flatMap { slot ->
                            (1..fillItem.defaultMaxStackSize).map { count ->
                                slot to count
                            }
                        }, modificationBlock = { (slot, count) ->
                            chest.setItem(slot, ItemStack(fillItem, count))
                        }, assertionBlock = { _, _ ->
                            with(helper.instruments) {
                                directPowerInstrument.assertRecords {
                                    for (attributes in sharedAttributes.values) {
                                        assertRecordsLong(attributes, 0L)
                                    }
                                }
                                powerInstrument.assertRecords {
                                    for (attributes in sharedAttributes.values) {
                                        assertRecordsLong(attributes, 0L)
                                    }
                                }
                            }
                        }).thenSucceed()
                }
            }
        }
    }
}
