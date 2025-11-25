package de.mctelemetry.core.utils

import io.wispforest.owo.ui.base.BaseParentComponent
import io.wispforest.owo.ui.core.Component
import net.minecraft.client.gui.components.AbstractWidget

inline operator fun <reified T : Component> BaseParentComponent.get(id: String): T? {
    return this.childById(id)
}

inline fun <reified T : Component> BaseParentComponent.childById(id: String): T? {
    return this.childById(T::class.java, id)
}

inline fun <reified T : Component> BaseParentComponent.childByIdOrThrow(id: String): T {
    return this.childById<T>(id) ?: throw NoSuchElementException("Could not find a ${T::class.simpleName} with id $id in $this")
}

inline fun <reified T: AbstractWidget> BaseParentComponent.childWidgetById(id: String): T? {
    return this.childById(
        @Suppress("UNCHECKED_CAST")
        // incorrect type information at compile time,
        // check is actually done correctly at runtime
        (T::class.java as Class<out Component>), id
    ) as T?
}

inline fun <reified T : AbstractWidget> BaseParentComponent.childWidgetByIdOrThrow(id: String): T {
    return this.childWidgetById(id) ?: throw NoSuchElementException("Could not find a ${T::class.java.simpleName} with id $id in $this")
}
