package de.mctelemetry.core.blocks

import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import de.mctelemetry.core.ui.screens.RedstoneScraperBlockScreen
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
        registerDefaultState(defaultBlockState().setValue(ERROR, ObservationSourceErrorState.Type.NotConfigured))
    }

    companion object {

        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING
        val ERROR: EnumProperty<ObservationSourceErrorState.Type> = EnumProperty.create(
            "error",
            ObservationSourceErrorState.Type::class.java,
        )
    }

    object RightClickBlockListener : InteractionEvent.RightClickBlock {

        override fun click(player: Player, hand: InteractionHand, pos: BlockPos, face: Direction): EventResult {
            val blockEntity = player.level().getBlockEntity(pos)
            if (blockEntity !is ObservationSourceContainerBlockEntity) {
                return EventResult.pass()
            }

            if (player.isShiftKeyDown) {
                if (player.getItemInHand(hand).item == Items.STICK) {
                    if (!player.level().isClientSide)
                        blockEntity.percussiveMaintenance()
                    return EventResult.interruptTrue()
                }
                return EventResult.pass()
            }

            if (player.level().isClientSide) {
                Minecraft.getInstance().execute {
                    Minecraft.getInstance().setScreen(RedstoneScraperBlockScreen(blockEntity))
                }
            }

            return EventResult.interruptTrue()
        }

        fun register() {
            InteractionEvent.RIGHT_CLICK_BLOCK.register(this)
        }
    }
}
