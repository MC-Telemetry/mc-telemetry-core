package de.mctelemetry.core.ui.datacomponents

import de.mctelemetry.core.TranslationKeys
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
                    TranslationKeys.Ui.stateOkay()
                }

                is ObservationSourceErrorState.Configured.Warnings -> {
                    TranslationKeys.Ui.stateWarning()
                }

                is ObservationSourceErrorState.Configured.Errors -> {
                    TranslationKeys.Ui.stateError()
                }
            }

            val tooltipComponent = if (errorState.errors.isNotEmpty() || errorState.warnings.isNotEmpty()) {
                listOf(
                    TranslationKeys.join(
                        TranslationKeys.Ui.and(),
                        TranslationKeys.Ui.stateErrorCount(errorState.errors.size),
                        TranslationKeys.Ui.stateWarningCount(errorState.warnings.size)
                    )
                ) + (errorState.errors + errorState.warnings).map {
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

    private data class TextTooltipPair(
        val textComponent: Component,
        val tooltipComponent: List<Component> = emptyList()
    )
}
