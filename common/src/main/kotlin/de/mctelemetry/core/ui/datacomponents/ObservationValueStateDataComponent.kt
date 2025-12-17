package de.mctelemetry.core.ui.datacomponents

import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder
import io.wispforest.owo.ui.component.LabelComponent
import net.minecraft.network.chat.Component

class ObservationValueStateDataComponent(
    private val label: LabelComponent,
    state: ObservationSourceState,
) : AutoCloseable {

    private val stateRegistration: AutoCloseable = state.subscribeToDirty(::update)

    init {
        update(state)
    }

    override fun close() {
        stateRegistration.close()
    }

    private fun update(state: ObservationSourceState) {
        val errorState = state.errorState
        val (textComponent, tooltipComponent) = if (errorState is ObservationSourceErrorState.Configured) {
            val textComponent = when (errorState) {
                ObservationSourceErrorState.Configured.Ok -> {
                    IComponentDSLBuilder.buildComponent("Ok")
                }

                is ObservationSourceErrorState.Configured.Warnings -> {
                    IComponentDSLBuilder.buildComponent("Warn")
                }

                is ObservationSourceErrorState.Configured.Errors -> {
                    IComponentDSLBuilder.buildComponent("Err")
                }
            }

            val tooltipComponent = if (errorState.errors.isNotEmpty() || errorState.warnings.isNotEmpty()) {
                listOf(
                    IComponentDSLBuilder.buildComponent(
                        andConcat(
                            numbering("Error", "Errors", errorState.errors.size),
                            numbering("Warning", "Warnings", errorState.warnings.size)
                        )
                    )) +
                        (errorState.errors + errorState.warnings).map {
                            IComponentDSLBuilder.buildComponent {
                                +"• "
                                +it
                            }
                        }
            } else {
                emptyList()
            }

            TextTooltipPair(
                textComponent,
                tooltipComponent,
            )
        } else {
            TextTooltipPair(IComponentDSLBuilder.buildComponent("-"))
        }


        label.text(textComponent)
        label.tooltip(tooltipComponent)
    }


    private fun numbering(singular: String, plural: String, count: Int): String {
        return if (count <= 0) {
            ""
        } else if (count == 1) {
            "1 $singular"
        } else {
            "$count $plural"
        }
    }

    private fun andConcat(vararg numbers: String): String {
        return numbers.filter { it.isNotEmpty() }.joinToString(" and ")
    }

    private data class TextTooltipPair(
        val textComponent: Component,
        val tooltipComponent: List<Component> = emptyList()
    )
}
