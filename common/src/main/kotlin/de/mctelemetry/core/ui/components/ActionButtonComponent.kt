package de.mctelemetry.core.ui.components

import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.color
import de.mctelemetry.core.utils.dsl.components.style
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.ParentComponent
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import net.minecraft.util.CommonColors

class ActionButtonComponent<T>(
    private val label: net.minecraft.network.chat.Component,
    private val title: net.minecraft.network.chat.Component,
    private val options: List<SelectBoxComponentEntry<T>>,
    private val onAction: (SelectBoxComponentEntry<T>) -> Unit
) : FlowLayout(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL) {

    private var overlayRef: ParentComponent? = null

    init {
        rebuild()
    }

    private fun rebuild() {
        clearChildren()

        child(
            (Components.button(label) { openDropdown() } as Component).apply {
                horizontalSizing(Sizing.expand())
            }
        )
    }

    private fun openDropdown() {
        if (overlayRef != null) {
            closeOverlay()
        }

        fun createEntry(list: FlowLayout, opt: SelectBoxComponentEntry<T>) {
            list.child(
                (Components.button(buildComponent {
                    append(opt.name)

                    if (opt.value == null) {
                        color(CommonColors.LIGHTER_GRAY)
                        style {
                            isItalic = true
                        }
                    }
                }) {
                    closeOverlay()
                    rebuild()
                    onAction(opt)
                } as Component).apply {
                    horizontalSizing(Sizing.expand())
                }
            )
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
        for (opt in options) {
            createEntry(entryList, opt)
        }

        val scrollBox = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(60), entryList)
        list.child(scrollBox)

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
