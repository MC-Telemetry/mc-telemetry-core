package de.mctelemetry.core.blocks

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IMappedAttributeKeyType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes
import de.mctelemetry.core.api.metrics.convertFrom
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.observationsync.ObservationSyncManagerClient
import de.mctelemetry.core.network.observations.container.observationsync.S2CObservationsPayloadObservationType
import de.mctelemetry.core.network.observations.container.settings.C2SObservationSourceSettingsUpdatePayload.Companion.sendConfigurationUpdate
import de.mctelemetry.core.observations.model.ObservationSourceConfiguration
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.storage.loot.LootParams


abstract class ObservationSourceContainerBlock(properties: Properties) : BaseEntityBlock(properties) {

    abstract override fun newBlockEntity(
        blockPos: BlockPos,
        blockState: BlockState,
    ): ObservationSourceContainerBlockEntity

    override fun getDrops(blockState: BlockState, builder: LootParams.Builder): List<ItemStack?>? {
        val item = asItem()
        return if (item == null)
            emptyList()
        else
            listOf(ItemStack(item))
    }

    override fun tick(
        blockState: BlockState,
        serverLevel: ServerLevel,
        blockPos: BlockPos,
        randomSource: RandomSource,
    ) {
        super.tick(blockState, serverLevel, blockPos, randomSource)
        val entity = serverLevel.getBlockEntity(blockPos)
        if (entity !is ObservationSourceContainerBlockEntity) {
            Util.logAndPauseIfInIde("Ticking unsupported entity $entity")
            return
        }
        entity.doBlockTick()
    }


    protected override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(ERROR)
    }

    init {
        registerDefaultState(stateDefinition.any())
        registerDefaultState(defaultBlockState().setValue(ERROR, ObservationSourceErrorState.Type.Errors))
    }

    companion object {

        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING
        val ERROR: EnumProperty<ObservationSourceErrorState.Type> = EnumProperty.create(
            "error",
            ObservationSourceErrorState.Type::class.java,
        )
    }

    object RightClickBlockListener : InteractionEvent.RightClickBlock {

        private fun onObservations(observationPayload: S2CObservationsPayloadObservationType) {
            for ((source, observations) in observationPayload) {
                OTelCoreMod.logger.info(
                    "Observations from {}:{}",
                    source,
                    if (observations.observations.isEmpty())
                        ""
                    else observations.observations.values.joinToString(
                        prefix = "\n\t- ",
                        separator = "\n\t- "
                    ) {
                        it.attributes.map.entries.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { (k, v) ->
                            "${k.baseKey.key}=${
                                NativeAttributeKeyTypes.StringType.convertFrom(
                                    k.type as IMappedAttributeKeyType<Any, *>,
                                    v
                                )
                            }"
                        } + ": " + buildString {
                            if (it.hasLong) {
                                append(it.longValue)
                                append('L')
                            }
                            if (it.hasDouble) {
                                append(it.doubleValue)
                                append("d")
                            }
                        }
                    })
            }
        }

        @Volatile
        private var configurationCopyPasteBuffer: Pair<Int, ObservationSourceConfiguration?>? = null

        override fun click(player: Player, hand: InteractionHand, pos: BlockPos, face: Direction): EventResult {
            val blockEntity = player.level().getBlockEntity(pos)
            if (blockEntity !is ObservationSourceContainerBlockEntity) {
                return EventResult.pass()
            }

            if (player.isShiftKeyDown) {
                when (player.getItemInHand(hand).item) {
                    Items.STICK -> {
                        if (!player.level().isClientSide)
                            blockEntity.percussiveMaintenance()
                        return EventResult.interruptTrue()
                    }
                    Items.PAPER -> {
                        if (player.level().isClientSide) {
                            val existingBufferValue = configurationCopyPasteBuffer
                            val entryWithConfiguration = blockEntity.observationStates.values.withIndex().firstOrNull {
                                it.value.configuration != null
                            }
                            configurationCopyPasteBuffer = if (entryWithConfiguration != null) {
                                OTelCoreMod.logger.info("Config found, copy paste buffer set to ${entryWithConfiguration.index}->${entryWithConfiguration.value}")
                                entryWithConfiguration.index to entryWithConfiguration.value.configuration
                            } else if (existingBufferValue != null) {
                                OTelCoreMod.logger.info("Value was stored but no config found, copy paste buffer set to ${existingBufferValue.first}->null")
                                existingBufferValue.first to null
                            } else {
                                OTelCoreMod.logger.info("No value was stored and no config found, copy paste buffer null")
                                null
                            }
                        }
                    }
                }
                return EventResult.pass()
            } else {
                when (player.getItemInHand(hand).item) {
                    Items.PAPER -> {
                        if (player.level().isClientSide) {
                            val buffer = configurationCopyPasteBuffer
                            if (buffer != null) {
                                val (index, value) = buffer
                                val state = blockEntity.observationStates.values.toList()[index]
                                OTelCoreMod.logger.info("Sending configuration update of ${blockEntity.level!!.dimension().location()}@${blockEntity.blockPos}/${state.source.id.location()} to $value")
                                state.sendConfigurationUpdate(
                                    GlobalPos(
                                        blockEntity.level!!.dimension(),
                                        blockEntity.blockPos,
                                    ),
                                    value
                                )
                            }
                            EventResult.interruptTrue()
                        }
                    }
                    Items.SPIDER_EYE -> {
                        @Suppress("OPT_IN_USAGE")
                        if (player.level().isClientSide) {
                            val job = GlobalScope.launch {
                                val observations = ObservationSyncManagerClient.getActiveManager()
                                    .requestObservations(GlobalPos(player.level().dimension(), pos), 20U)
                                (observations.onStart { emit(observations.value) }).collect { observationPayload ->
                                    onObservations(observationPayload)
                                }
                            }
                            GlobalScope.launch {
                                delay(60000)
                                job.cancelAndJoin()
                            }
                        }
                    }
                }
            }

            return EventResult.interruptTrue()
        }

        fun register() {
            InteractionEvent.RIGHT_CLICK_BLOCK.register(this)
        }
    }
}
