package de.mctelemetry.core.gametest.observation.scraper.redstone

import de.mctelemetry.core.utils.doubleInstrument
import de.mctelemetry.core.utils.gametest.observation.InstrumentGameTestHelper.Companion.instruments
import de.mctelemetry.core.utils.gametest.observation.withConfiguredStartupSequence
import de.mctelemetry.core.utils.longInstrument
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

// Classes containing @GameTest or @GameTestFactory need to be added to CommonGameTestFactory to be detected.
object RedstoneScraperBlockAirTest {

    private val AirBlockPos: BlockPos = BlockPos(0, 1, 1)

    object Undirected {

        @Suppress("unused")
        @JvmStatic
        @GameTest(template = "mcotelcore:redstonescraper.air")
        fun longScrapeAirTest(helper: GameTestHelper) {
            helper.withConfiguredStartupSequence { sequence, instruments ->
                val comparatorInstrument = instruments.getValue("comparator").longInstrument
                val directPowerInstrument = instruments.getValue("direct_power").longInstrument
                val powerInstrument = instruments.getValue("power").longInstrument
                with(helper.instruments) {
                    comparatorInstrument.assertRecordsNone(supportsFloating = false)
                    directPowerInstrument.assertRecordsSingle(
                        Attributes.of(
                            AttributeKey.stringArrayKey("pos"),
                            helper.instruments.formatGlobalBlockPos(AirBlockPos),
                        ), 0L
                    )
                    powerInstrument.assertRecordsSingle(
                        Attributes.of(
                            AttributeKey.stringArrayKey("pos"),
                            helper.instruments.formatGlobalBlockPos(AirBlockPos),
                        ), 0L
                    )
                }
                helper.succeed()
            }
        }

        @Suppress("unused")
        @JvmStatic
        @GameTest(template = "mcotelcore:redstonescraper.air")
        fun doubleScrapeAirTest(helper: GameTestHelper) {
            helper.withConfiguredStartupSequence(useDouble = { true }) { sequence, instruments ->
                val comparatorInstrument = instruments.getValue("comparator").doubleInstrument
                with(helper.instruments) {
                    comparatorInstrument.assertRecordsNone()
                }
                helper.succeed()
            }
        }
    }

    object Directed {

        @Suppress("unused")
        @JvmStatic
        @GameTest(template = "mcotelcore:redstonescraper.air.directed")
        fun longScrapeAirTest(helper: GameTestHelper) {
            helper.withConfiguredStartupSequence { sequence, instruments ->
                val directPowerInstrument = instruments.getValue("direct_power").longInstrument
                val powerInstrument = instruments.getValue("power").longInstrument
                with(helper.instruments) {
                    directPowerInstrument.assertRecords {
                        for (direction in Direction.entries) {
                            assertRecordsLong(
                                Attributes.of(
                                    AttributeKey.stringArrayKey("pos"),
                                    helper.instruments.formatGlobalBlockPos(AirBlockPos),
                                    AttributeKey.stringKey("dir"),
                                    helper.instruments.formatDirection(direction),
                                ), 0L
                            )
                        }
                    }
                    powerInstrument.assertRecords {
                        for (direction in Direction.entries) {
                            assertRecordsLong(
                                Attributes.of(
                                    AttributeKey.stringArrayKey("pos"),
                                    helper.instruments.formatGlobalBlockPos(AirBlockPos),
                                    AttributeKey.stringKey("dir"),
                                    helper.instruments.formatDirection(direction),
                                ), 0L
                            )
                        }
                    }
                }
                helper.succeed()
            }
        }
    }
}
