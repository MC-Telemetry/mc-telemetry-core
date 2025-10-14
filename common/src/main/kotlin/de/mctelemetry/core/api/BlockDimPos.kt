package de.mctelemetry.core.api

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level

@JvmRecord
data class BlockDimPos(val dimension: ResourceKey<Level>, val position: BlockPos) {

    context(server: MinecraftServer)
    val level: ServerLevel?
        get() {
            return server.getLevel(dimension)
        }

    context(server: MinecraftServer)
    val loaded: Boolean
        get() {
            return context(level ?: return false) {
                position.loaded
            }
        }

    context(server: MinecraftServer)
    val loadedAndShouldTick: Boolean
        get() {
            return context(level ?: return false) {
                position.loadedAndShouldTick
            }
        }
}

context(level: Level)
val BlockPos.loaded: Boolean
    get() = level.isLoaded(this)

context(level: Level)
val BlockPos.loadedAndShouldTick: Boolean
    get() = level.isLoaded(this) && level.shouldTickBlocksAt(this)
