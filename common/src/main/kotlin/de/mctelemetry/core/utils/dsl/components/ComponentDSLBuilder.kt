package de.mctelemetry.core.utils.dsl.components

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

@PublishedApi
internal class ComponentDSLBuilder(
    val component: MutableComponent,
) : IComponentDSLBuilder {

    override val contents: ComponentContents
        get() = component.contents
    override val siblings: List<Component>
        get() = component.siblings
    override var style: Style
        get() = component.style
        set(value) {
            component.style = value
        }

    override fun append(component: Component) {
        this.component.append(component)
    }

    override fun append(text: String) {
        this.component.append(text)
    }

    override fun toString(): String {
        return "${ComponentDSLBuilder::class.simpleName}($component)"
    }
}
