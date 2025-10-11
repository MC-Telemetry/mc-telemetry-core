package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.managar.IGameMetricsManager
import de.mctelemetry.core.api.metrics.managar.IWorldMetricsManager
import net.minecraft.server.MinecraftServer

class WorldMetricsManager(
    override val gameMetrics: IGameMetricsManager,
    val server: MinecraftServer,
) : IWorldMetricsManager {
    private var active: Boolean = false

    fun start() {
        if(active) return
        active = true
    }
    fun stop() {
        if(!active) return
        active = false
    }
}
