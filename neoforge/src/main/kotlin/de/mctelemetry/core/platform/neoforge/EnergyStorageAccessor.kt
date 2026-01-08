package de.mctelemetry.core.platform.neoforge

import de.mctelemetry.core.platform.IEnergyStorageAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.capabilities.Capabilities

object EnergyStorageAccessor : IEnergyStorageAccessor {
    override fun getEnergyAmount(level: ServerLevel, position: BlockPos, facing: Direction?): Long {
        val cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, position, facing) ?: return 0

        return cap.energyStored.toLong()
    }

    override fun getEnergyCapacity(level: ServerLevel, position: BlockPos, facing: Direction?): Long {
        val cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, position, facing) ?: return 0

        return cap.maxEnergyStored.toLong()
    }

    override fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double {
        val cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, position, facing) ?: return 0.0

        return cap.energyStored.toDouble() / cap.maxEnergyStored.toDouble()
    }
}
