package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.childByIdOrThrow
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.container.FlowLayout
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class InstrumentManagerScreen() : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "instrument-manager"
        )
    )
) {

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun build(rootComponent: FlowLayout) {
        val list: FlowLayout = rootComponent.childByIdOrThrow("list")
    }
}
