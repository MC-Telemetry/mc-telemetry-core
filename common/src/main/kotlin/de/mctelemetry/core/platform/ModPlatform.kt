package de.mctelemetry.core.platform

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel

interface ModPlatform {

    fun getPlatformName(): String

    fun getItemStorageAccessor(level: ServerLevel, position: BlockPos, facing: Direction?): IItemStorageAccessor

    companion object : ModPlatform by ModPlatformProvider.getPlatform()
}
