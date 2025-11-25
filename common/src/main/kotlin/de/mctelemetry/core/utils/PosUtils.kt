package de.mctelemetry.core.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.Vec3i
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3


fun GlobalPos.toShortString(): String {
    return "${dimension.location()}@(${pos.toShortString()})"
}

operator fun GlobalPos.component1(): ResourceKey<Level> = dimension()
operator fun GlobalPos.component2(): BlockPos = pos
operator fun Vec3i.component1(): Int = x
operator fun Vec3i.component2(): Int = y
operator fun Vec3i.component3(): Int = z
operator fun Vec3.component1(): Double = x
operator fun Vec3.component2(): Double = y
operator fun Vec3.component3(): Double = y
