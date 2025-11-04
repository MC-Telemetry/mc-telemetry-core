package de.mctelemetry.core.utils

import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.ThrowingComponent
import java.util.Optional

internal data class ExceptionComponent(
    val exception: Exception,
    val component: Component,
) : Component by component {

    constructor(exception: Exception) : this(
        exception,
        when (exception) {
            is Component -> exception
            is ThrowingComponent -> exception.component
            else -> buildComponent(
                exception.javaClass.simpleName
            ) {
                val message = exception.message
                if (message != null) {
                    +": "
                    append(message)
                }
            }
        })

    override fun getString(): String? {
        return component.string
    }

    override fun getString(i: Int): String? {
        return component.getString(i)
    }

    override fun tryCollapseToString(): String? {
        return component.tryCollapseToString()
    }

    override fun plainCopy(): MutableComponent? {
        return component.plainCopy()
    }

    override fun copy(): MutableComponent? {
        return component.copy()
    }

    override fun <T : Any?> visit(
        styledContentConsumer: FormattedText.StyledContentConsumer<T?>,
        style: Style,
    ): Optional<T?>? {
        return component.visit(styledContentConsumer, style)
    }

    override fun <T : Any?> visit(contentConsumer: FormattedText.ContentConsumer<T?>): Optional<T?>? {
        return component.visit(contentConsumer)
    }

    override fun toFlatList(): List<Component?>? {
        return component.toFlatList()
    }

    override fun toFlatList(style: Style): List<Component?>? {
        return component.toFlatList(style)
    }

    override fun contains(component: Component): Boolean {
        return component.contains(component)
    }
}
