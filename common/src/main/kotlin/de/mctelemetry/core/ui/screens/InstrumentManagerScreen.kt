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
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class InstrumentManagerScreen(private val instrumentManager: IClientWorldInstrumentManager) :
    BaseUIModelScreen<FlowLayout>(
        FlowLayout::class.java, DataSource.asset(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID, "instrument-manager"
            )
        )
    ), AutoCloseable {

    private val closable = mutableListOf<AutoCloseable>()

    private lateinit var instrumentList: FlowLayout

    init {
        closable.add(
            instrumentManager.addLocalCallback(
                object : IInstrumentAvailabilityCallback<IInstrumentDefinition> {
                    override fun instrumentAdded(
                        manager: IInstrumentManager,
                        instrument: IInstrumentDefinition,
                        phase: IInstrumentAvailabilityCallback.Phase
                    ) {
                        updateInstrumentList()
                    }

                    override fun instrumentRemoved(
                        manager: IInstrumentManager,
                        instrument: IInstrumentDefinition,
                        phase: IInstrumentAvailabilityCallback.Phase
                    ) {
                        updateInstrumentList()
                    }
                })
        )
    }

    override fun close() {
        closable.closeConsumeAllRethrow()
    }

    override fun onClose() {
        @Suppress("ConvertTryFinallyToUseCall")
        try {
            super.onClose()
        } finally {
            this.close()
        }
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun build(rootComponent: FlowLayout) {
        instrumentList = rootComponent.childByIdOrThrow("list")
        updateInstrumentList()
    }

    private fun updateInstrumentList() {
        instrumentList.clearChildren()

        val instruments = instrumentManager.findLocal().filter { it.persistent }.sortedBy { it.name }
        for (instrument in instruments) {
            val template = model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:instrument-manager",
                mapOf()
            )
            template.childByIdOrThrow<LabelComponent>("instrument-name").text(
                buildComponent(instrument.name)
            )
            instrumentList.child(template)

            val deleteButton: ButtonComponent = template.childWidgetByIdOrThrow("instrument-delete")
            deleteButton.onPress {
                instrumentManager.requestInstrumentRemoval(instrument.name)
            }
        }
    }
}
