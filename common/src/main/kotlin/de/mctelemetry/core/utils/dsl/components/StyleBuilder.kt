package de.mctelemetry.core.utils.dsl.components

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceLocation

@PublishedApi
internal class StyleBuilder(
    var style: Style = Style.EMPTY,
) : IStyleBuilder {


    override var color: TextColor?
        get() = style.color
        set(value) {
            style = style.withColor(value)
        }
    override var isBold: Boolean?
        get() = style.isBold
        set(value) {
            style = style.withBold(value)
        }
    override var isItalic: Boolean?
        get() = style.isItalic
        set(value) {
            style.withItalic(value)
        }
    override var isUnderlined: Boolean?
        get() = style.isUnderlined
        set(value) {style = style.withUnderlined(value)}
    override var isStrikethrough: Boolean?
        get() = style.isStrikethrough
        set(value) {style = style.withStrikethrough(value)}
    override var isObfuscated: Boolean?
        get() = style.isObfuscated
        set(value) {style = style.withObfuscated(value)}
    override var clickEvent: ClickEvent?
        get() = style.clickEvent
        set(value) {style = style.withClickEvent(value)}
    override var hoverEvent: HoverEvent?
        get() = style.hoverEvent
        set(value) {style = style.withHoverEvent(value)}
    override var insertion: String?
        get() = style.insertion
        set(value) {style = style.withInsertion(value)}
    override var font: ResourceLocation?
        get() = style.font
        set(value) {style = style.withFont(value)}

    override fun isEmpty(): Boolean {
        return style.isEmpty
    }

    override fun color(formatting: ChatFormatting?) {
        style = style.withColor(formatting)
    }

    override fun color(color: Int) {
        style = style.withColor(color)
    }

    override fun toString(): String {
        return "${StyleBuilder::class.simpleName}($style)"
    }
}
