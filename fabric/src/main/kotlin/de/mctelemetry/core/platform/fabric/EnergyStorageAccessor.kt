package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IEnergyStorageAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import team.reborn.energy.api.EnergyStorage

object EnergyStorageAccessor : IEnergyStorageAccessor {
    override fun getEnergyAmount(level: ServerLevel, position: BlockPos, facing: Direction?): Long {
        val storage = EnergyStorage.SIDED.find(level, position, facing) ?: return 0

        return storage.amount
    }

    override fun getEnergyCapacity(level: ServerLevel, position: BlockPos, facing: Direction?): Long {
        val storage = EnergyStorage.SIDED.find(level, position, facing) ?: return 0

        return storage.capacity
    }

    override fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double {
        val storage = EnergyStorage.SIDED.find(level, position, facing) ?: return 0.0

        return storage.amount.toDouble() / storage.capacity.toDouble()
    }
}
