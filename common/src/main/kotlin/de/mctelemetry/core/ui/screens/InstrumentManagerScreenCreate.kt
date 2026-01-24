package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.attributes.create
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.sendGaugeInstrument
import de.mctelemetry.core.ui.components.AttributeCreatorComponent
import de.mctelemetry.core.ui.components.AttributeCreatorEntry
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import io.github.pixix4.kobserve.list.observableListOf
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.SmallCheckboxComponent
import io.wispforest.owo.ui.component.TextBoxComponent
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
        val saveButton = rootComponent.childWidgetByIdOrThrow<ButtonComponent>("save")

        val instrumentNameTextBox = rootComponent.childWidgetByIdOrThrow<TextBoxComponent>("instrument-name")
        val useDecimalsCheckBox = rootComponent.childByIdOrThrow<SmallCheckboxComponent>("instrument-use-decimals")

        val attributesList = observableListOf<AttributeCreatorEntry>()
        val layout = rootComponent.childByIdOrThrow<FlowLayout>("attribute-creator")
        layout.child(AttributeCreatorComponent(attributesList))

        saveButton.onPress {
            Minecraft.getInstance().setScreen(parent)

            val name = instrumentNameTextBox.value
            val useDecimals = useDecimalsCheckBox.checked()

            instrumentManager.sendGaugeInstrument(name) {
                supportsFloating = useDecimals
                persistent = true
                for(attribute in attributesList) {
                    addAttribute(attribute.type.create(attribute.name, null))
                }
            }
        }
    }
}
