package de.mctelemetry.core.ui.components

import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.observations.IParameterizedObservationSource
import de.mctelemetry.core.api.observations.IParameterizedObservationSource.Parameter.Companion.setFromText
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.ParentComponent
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment

class ParameterInputComponent(
    private val parent: FlowLayout,
    private val title: net.minecraft.network.chat.Component,
    private val commitLabel: net.minecraft.network.chat.Component,
    private val parameters: IParameterizedObservationSource.ParameterMap,
    private val onCommit: (IParameterizedObservationSource.ParameterMap) -> Unit
) {
    private var overlayRef: ParentComponent? = null

    init {
        openDropdown()
    }

    private fun openDropdown() {
        if (overlayRef != null) {
            closeOverlay()
        }

        val list = Containers.verticalFlow(Sizing.fill(36), Sizing.content())
        list.gap(4)
        list.surface(Surface.BLANK)
        list.horizontalAlignment(HorizontalAlignment.CENTER)

        val titleLabel = Components.label(title)
        titleLabel.margins(Insets.bottom(2))
        list.child(titleLabel)

        val entryList = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
        entryList.gap(4)
        entryList.padding(Insets.of(4))
        entryList.surface(Surface.BLANK)
        entryList.horizontalAlignment(HorizontalAlignment.CENTER)

        val textBoxes = context(parameters) {
            parameters.availableParameters.mapValues { (_, parameter) ->
                parameter to createEntry(entryList, parameter)
            }
        }

        val scrollBox = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(60), entryList)
        list.child(scrollBox)

        list.child(
            (Components.button(commitLabel) {
                try {
                    context(parameters) {
                        textBoxes.values.forEach { (parameter, textBox) ->
                            parameter.setFromText(textBox.value)
                        }
                    }
                    onCommit(parameters)
                } catch (e: CommandSyntaxException) {
                    OTelCoreMod.logger.error("Cannot parse parameters", e)
                } finally {
                    closeOverlay()
                }
            } as Component).apply {
                horizontalSizing(Sizing.expand())
            }
        )

        val overlay = Containers.overlay(list).apply {
            zIndex(500)
        }

        val rootComponent = parent.root() as? FlowLayout ?: return
        rootComponent.child(overlay)

        overlayRef = overlay
    }

    private fun closeOverlay() {
        val overlay = overlayRef ?: return

        val rootComponent = parent.root() as? FlowLayout ?: return
        rootComponent.removeChild(overlay)

        overlayRef = null
    }

    context(parameterMap: IParameterizedObservationSource.ParameterMap)
    private fun <T> createEntry(
        list: FlowLayout,
        parameter: IParameterizedObservationSource.Parameter<T>
    ): TextBoxComponent {
        val entryList = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
        entryList.verticalAlignment(VerticalAlignment.CENTER)
        entryList.horizontalAlignment(HorizontalAlignment.RIGHT)

        val nameLabel = Components.label(parameter.displayName)
        nameLabel.margins(Insets.right(4))
        entryList.child(nameLabel)

        val textBox = ParameterValidationTextBoxComponent(parameter, Sizing.fill(60))
        entryList.child(textBox as Component)

        list.child(entryList)

        return textBox
    }
}
