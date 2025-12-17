package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.closeConsumeAllRethrow
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.FlowLayout
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class InstrumentManagerScreenCreate(
    private val parent: Screen,
    private val instrumentManager: IClientWorldInstrumentManager
) :
    BaseUIModelScreen<FlowLayout>(
        FlowLayout::class.java, DataSource.asset(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID, "instrument-manager-create"
            )
        )
    ) {

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun build(rootComponent: FlowLayout) {
        val backButton = rootComponent.childWidgetByIdOrThrow<ButtonComponent>("back")
        backButton.onPress {
            Minecraft.getInstance().setScreen(parent)
        }

        val list = rootComponent.childByIdOrThrow<FlowLayout>("list")
    }
}
