package de.mctelemetry.core.utils.dsl.components

import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import java.util.Optional
import java.util.UUID

operator fun Style.plus(style: Style): Style = style.applyTo(this)

operator fun ClickEvent.Action.invoke(value: String) = ClickEvent(this, value)

operator fun <T : Any> HoverEvent.Action<T>.invoke(value: T): HoverEvent = HoverEvent(this, value)
operator fun HoverEvent.Action<Component>.invoke() = this(Component.empty())
operator fun HoverEvent.Action<Component>.invoke(text: String) = this(Component.literal(text))

operator fun HoverEvent.Action<Component>.invoke(block: IComponentDSLBuilder.() -> Unit) = this(buildComponent(block))

operator fun HoverEvent.Action<HoverEvent.ItemStackInfo>.invoke(value: ItemStack) =
    this(HoverEvent.ItemStackInfo(value))

operator fun HoverEvent.Action<HoverEvent.EntityTooltipInfo>.invoke(entityType: EntityType<*>, uuid: UUID) =
    this(HoverEvent.EntityTooltipInfo(entityType, uuid, Optional.empty()))

operator fun HoverEvent.Action<HoverEvent.EntityTooltipInfo>.invoke(
    entityType: EntityType<*>,
    uuid: UUID,
    name: Component,
) = this(HoverEvent.EntityTooltipInfo(entityType, uuid, name))

operator fun HoverEvent.Action<HoverEvent.EntityTooltipInfo>.invoke(
    entityType: EntityType<*>,
    uuid: UUID,
    name: String,
) = this(HoverEvent.EntityTooltipInfo(entityType, uuid, Component.literal(name)))

operator fun HoverEvent.Action<HoverEvent.EntityTooltipInfo>.invoke(
    entityType: EntityType<*>,
    uuid: UUID,
    nameBlock: IComponentDSLBuilder.() -> Unit,
) = this(HoverEvent.EntityTooltipInfo(entityType, uuid, buildComponent(nameBlock)))
