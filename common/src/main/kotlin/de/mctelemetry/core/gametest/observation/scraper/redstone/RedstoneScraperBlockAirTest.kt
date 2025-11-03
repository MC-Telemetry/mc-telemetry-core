package de.mctelemetry.core.gametest.observation.scraper.redstone

import com.mojang.datafixers.util.Either
import de.mctelemetry.core.api.metrics.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.metrics.ILongInstrumentRegistration
import de.mctelemetry.core.api.metrics.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.blocks.RedstoneScraperBlock
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.gametest.InstrumentGameTestHelper.Companion.instruments
import de.mctelemetry.core.utils.gametest.failC
import de.mctelemetry.core.utils.gametest.thenExecuteAfterC
import de.mctelemetry.core.utils.gametest.thenExecuteC
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.world.level.block.Block

object RedstoneScraperBlockAirTest {

    private val ScraperBlock: Block by lazy {
        OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get()
    }

    private val ScraperBlockPos: BlockPos = BlockPos(0, 1, 0)

    private fun GameTestHelper.withConfiguredStartupSequence(
        useDouble: (originalName: String) -> Boolean = { false },
        suffixLimit: Int = 1024,
        overrideExisting: Boolean = true,
        customizer: IGaugeInstrumentBuilder<*>.(originalName: String, isDouble: Boolean) -> Unit = { _, _ -> },
        block: (GameTestSequence, Map<String, Either<ILongInstrumentRegistration.Mutable<*>, IDoubleInstrumentRegistration.Mutable<*>>>) -> Unit,
    ) {
        var result: Map<String,
                Either<ILongInstrumentRegistration.Mutable<*>,
                        IDoubleInstrumentRegistration.Mutable<*>>>? = null
        var sequence: GameTestSequence? = null
        @Suppress("AssignedValueIsNeverRead")
        // assigned value is actually being read in callback after construction of its value
        sequence = startSequence()
            .thenExecuteAfterC(2) {
                BlockPos.betweenClosedStream(bounds.contract(1.0, 1.0, 1.0)).forEachOrdered { blockPos ->
                    val state = getBlockState(blockPos)
                    if (state.block !== ScraperBlock) return@forEachOrdered
                    val errorStateValue = state.getValue(RedstoneScraperBlock.ERROR)
                    if (errorStateValue != ObservationSourceState.ErrorState.Type.Errors) {
                        failC(
                            "Unexpected error state: Expected ${ObservationSourceState.ErrorState.Type.Errors}, got $errorStateValue",
                            blockPos
                        )
                    }
                }
                result = instruments.configureObservationContainers(
                    useDouble = useDouble,
                    suffixLimit = suffixLimit,
                    overrideExisting = overrideExisting,
                    customizer = customizer,
                )
            }.thenExecuteAfterC(2) {
                BlockPos.betweenClosedStream(bounds.contract(1.0, 1.0, 1.0)).forEachOrdered { blockPos ->
                    val state = getBlockState(ScraperBlockPos)
                    if (state.block !== ScraperBlock) return@forEachOrdered
                    val errorStateValue = state.getValue(RedstoneScraperBlock.ERROR)
                    if (errorStateValue != ObservationSourceState.ErrorState.Type.Ok) {
                        failC(
                            "Unexpected error state: Expected ${ObservationSourceState.ErrorState.Type.Ok}, got $errorStateValue",
                            ScraperBlockPos
                        )
                    }
                }
                @Suppress("KotlinConstantConditions")
                // Suppress reported that sequence is always null, which is simply not true because this block is only
                // executed after the whole containing method already returns.
                block(sequence!!, result!!)
            }.thenExecuteC { } // filler node so that iterator of sequence does not report empty before block is done
    }

    object Undirected {

        @Suppress("unused")
        @JvmStatic
        @GameTest(template = "mcotelcore:redstonescraper.air")
        fun longScrapeAirTest(helper: GameTestHelper) {
            helper.withConfiguredStartupSequence { sequence, instruments ->
                val comparatorInstrument: ILongInstrumentRegistration = instruments.getValue("comparator").left().get()
                val directPowerInstrument: ILongInstrumentRegistration =
                    instruments.getValue("direct_power").left().get()
                val powerInstrument: ILongInstrumentRegistration = instruments.getValue("power").left().get()
                with(helper.instruments) {
                    comparatorInstrument.assertRecordsNone(supportsFloating = false)
                    directPowerInstrument.assertRecordsSingle(
                        Attributes.of(
                            AttributeKey.stringArrayKey("pos"),
                            helper.instruments.formatGlobalBlockPos(ScraperBlockPos),
                        ), 0L
                    )
                    powerInstrument.assertRecordsSingle(
                        Attributes.of(
                            AttributeKey.stringArrayKey("pos"),
                            helper.instruments.formatGlobalBlockPos(ScraperBlockPos),
                        ), 0L
                    )
                }
                helper.succeed()
            }
        }
    }
}
