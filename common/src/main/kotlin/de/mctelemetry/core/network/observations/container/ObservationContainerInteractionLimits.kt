package de.mctelemetry.core.network.observations.container

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import org.apache.logging.log4j.LogManager
import kotlin.contracts.contract
import kotlin.math.abs

object ObservationContainerInteractionLimits {

    private val subLogger =
        LogManager.getLogger("${OTelCoreMod.MOD_ID}.${ObservationContainerInteractionLimits::class.simpleName}")

    const val RANGE: Double = 40.0

    fun checkIsInteractable(
        level: Level?,
        pos: BlockPos,
        checkLoaded: Boolean = true,
        checkBlockEntity: Boolean = true,
    ): Boolean {
        contract {
            returns(true) implies (level != null)
        }
        if (level == null) return false
        if (checkLoaded || checkBlockEntity) {
            if (!level.isLoaded(pos)) return false
            if (checkBlockEntity) {
                val blockEntity = level.getBlockEntity(pos)
                if (blockEntity !is ObservationSourceContainerBlockEntity) {
                    return false
                }
            }
        }
        return true
    }

    fun checkCanInteract(
        player: Player,
        dimension: ResourceKey<Level>,
        pos: BlockPos,
        log: Boolean = false,
        checkDimension: Boolean = true,
        checkDistance: Boolean = true,
        checkLoaded: Boolean = true,
        checkBlockEntity: Boolean = true,
    ): Boolean {
        if (player.isRemoved) {
            if (log) {
                subLogger.trace("Player cannot interact because they have been removed: {}", player)
            }
            return false
        }
        val playerLevel = player.level()
        if (checkDimension && playerLevel.dimension() != dimension) {
            if (log) {
                subLogger.trace(
                    "Player cannot interact because of dimension mismatch: Tried to interact with {} in {} but player is in {}",
                    pos,
                    dimension,
                    playerLevel.dimension()
                )
            }
            return false
        }
        if (checkDistance) {
            val playerPos = player.position()
            val deltaX = pos.x + 0.5 - playerPos.x
            val deltaZ = pos.z + 0.5 - playerPos.z
            if (deltaX > RANGE || deltaX < -RANGE || deltaZ > RANGE || deltaZ < -RANGE) {
                if (log) {
                    subLogger.trace(
                        "Player cannot interact because of distance: Tried to interact with {} in {} but player is {},{} (x,z) blocks away",
                        pos,
                        dimension,
                        abs(deltaX),
                        abs(deltaZ),
                    )
                }
                return false
            }
        }
        if ((checkLoaded || checkBlockEntity) && !playerLevel.isLoaded(pos)) {
            if (log) {
                subLogger.trace(
                    "Player cannot interact because position is unloaded: Tried to interact with {} in {}",
                    pos,
                    dimension,
                )
            }
            return false
        } else if (checkBlockEntity) {
            val blockEntity = playerLevel.getBlockEntity(pos)
            if (blockEntity !is ObservationSourceContainerBlockEntity) {
                if (log) {
                    subLogger.trace(
                        "Player cannot interact because position does not contain an {}: Tried to interact with {} in {} is {}",
                        ObservationSourceContainerBlockEntity::class.java.simpleName,
                        pos,
                        dimension,
                        blockEntity
                    )
                }
                return false
            }
        }
        return true
    }
}
