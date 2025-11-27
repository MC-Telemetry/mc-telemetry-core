package de.mctelemetry.core.ui.components

import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.ParentComponent
import io.wispforest.owo.ui.core.Sizing

class SelectBoxComponent<T>(
    private val options: List<T>,
    initial: T?,
    private val display: (T) -> String = { it.toString() },
    private val onChange: (T?, T?) -> Unit
) : FlowLayout(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL) {

    private var selected: T? = initial
    private var overlayRef: ParentComponent? = null

    init {
        rebuild()
    }

    private fun rebuild() {
        clearChildren()

        val displayText = selected?.run(display) ?: "---"
        child(
            Components.button(
                buildComponent { +displayText }
            ) { openDropdown() } as Component
        )
    }

    private fun openDropdown() {
        if (overlayRef != null) {
            closeOverlay()
        }

        fun createEntry(list: FlowLayout, opt: T?) {
            val displayText = opt?.run(display) ?: "---"

            list.child(
                Components.button(buildComponent { +displayText }) {
                    val oldValue = selected
                    selected = opt
                    onChange(oldValue, opt)
                    rebuild()
                    closeOverlay()
                } as Component
            )
        }

        val list = Containers.verticalFlow(Sizing.content(), Sizing.content())
        list.gap(4)

        createEntry(list, null)
        for (opt in options) {
            createEntry(list, opt)
        }

        val overlay = Containers.overlay(list).apply {
            zIndex(500)
        }

        val rootComponent = this.root() as? FlowLayout ?: return
        rootComponent.child(overlay)

        overlayRef = overlay
    }

    private fun closeOverlay() {
        val overlay = overlayRef ?: return

        val rootComponent = this.root() as? FlowLayout ?: return
        rootComponent.removeChild(overlay)

        overlayRef = null
    }
}
