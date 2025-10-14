package de.mctelemetry.core.utils.dsl.components

import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.IStyleBuilder.Companion.buildStyle
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

@ComponentDSL
interface IComponentDSLBuilder {

    val contents: ComponentContents
    val siblings: List<Component>
    var style: Style

    fun setStyle(block: IStyleBuilder.() -> Unit) {
        style = buildStyle(block)
    }

    fun append(component: Component)
    fun append(text: String) = append(Component.literal(text))
    operator fun Component.unaryPlus() = append(this)
    operator fun String.unaryPlus() = append(this)


    companion object {

        @ComponentDSL
        fun buildComponent(): MutableComponent {
            return Component.empty()
        }
        @ComponentDSL
        inline fun buildComponent(block: IComponentDSLBuilder.() -> Unit): MutableComponent {
            return ComponentDSLBuilder(Component.empty()).apply(block).component
        }

        @ComponentDSL
        inline fun buildComponent(text: String, block: IComponentDSLBuilder.() -> Unit): MutableComponent {
            return ComponentDSLBuilder(Component.literal(text)).apply(block).component
        }
    }
}

@ComponentDSL
inline fun IComponentDSLBuilder.applyStyle(block: IStyleBuilder.() -> Unit) {
    style += buildStyle(block)
}

@ComponentDSL
inline fun IComponentDSLBuilder.style(block: IStyleBuilder.() -> Unit) {
    style = buildStyle(style, block)
}

@ComponentDSL
inline fun IComponentDSLBuilder.append(block: IComponentDSLBuilder.() -> Unit) = append(buildComponent(block))

@ComponentDSL
inline fun IComponentDSLBuilder.append(text: String, block: IComponentDSLBuilder.() -> Unit) =
    append(buildComponent(text, block))

@Suppress("NOTHING_TO_INLINE")
@ComponentDSL
context(builder: IComponentDSLBuilder)
inline operator fun (IComponentDSLBuilder.() -> Unit).unaryPlus() = builder.append(this)

@ComponentDSL
context(_: IComponentDSLBuilder)
inline operator fun String.invoke(block: IComponentDSLBuilder.() -> Unit) = buildComponent(this, block)

@ComponentDSL
fun IComponentDSLBuilder.color(color: Int) = applyStyle { color(color) }
