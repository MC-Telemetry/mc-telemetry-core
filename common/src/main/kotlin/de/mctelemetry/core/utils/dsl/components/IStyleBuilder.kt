package de.mctelemetry.core.utils.dsl.components

import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import java.util.UUID

@ComponentDSL
interface IStyleBuilder {

    var color: TextColor?
    var isBold: Boolean?
    var isItalic: Boolean?
    var isUnderlined: Boolean?
    var isStrikethrough: Boolean?
    var isObfuscated: Boolean?
    var clickEvent: ClickEvent?
    var hoverEvent: HoverEvent?
    var insertion: String?
    var font: ResourceLocation?

    fun isEmpty(): Boolean

    fun IStyleBuilder.clickEvent(action: ClickEvent.Action, value: String) {
        clickEvent = ClickEvent(action, value)
    }

    fun <T : Any> IStyleBuilder.hoverEvent(action: HoverEvent.Action<T>, arg: T) {
        hoverEvent = HoverEvent(action, arg)
    }

    fun color(formatting: ChatFormatting?)
    fun color(color: Int)

    companion object {

        @ComponentDSL
        inline fun buildStyle(block: IStyleBuilder.() -> Unit): Style {
            return StyleBuilder().apply(block).style
        }
        @ComponentDSL
        inline fun buildStyle(style: Style, block: IStyleBuilder.() -> Unit): Style {
            return StyleBuilder(style).apply(block).style
        }
    }
}

@ComponentDSL
fun IStyleBuilder.onClickOpenURL(url: String) = clickEvent(ClickEvent.Action.OPEN_URL, url)
@ComponentDSL
fun IStyleBuilder.onClickOpenFile(path: String) = clickEvent(ClickEvent.Action.OPEN_FILE, path)
@ComponentDSL
fun IStyleBuilder.onClickRunCommand(command: String) = clickEvent(ClickEvent.Action.RUN_COMMAND, command)
@ComponentDSL
fun IStyleBuilder.onClickSuggestCommand(command: String) = clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)
@ComponentDSL
fun IStyleBuilder.onClickChangePage(page: Int) = clickEvent(ClickEvent.Action.CHANGE_PAGE, page.toString())
@ComponentDSL
fun IStyleBuilder.onClickCopyToClipboard(text: String) = clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)

@ComponentDSL
fun IStyleBuilder.onHoverShowText(component: Component) = hoverEvent(HoverEvent.Action.SHOW_TEXT, component)
@ComponentDSL
fun IStyleBuilder.onHoverShowText(text: String) = onHoverShowText(Component.literal(text))

@ComponentDSL
inline fun IStyleBuilder.onHoverShowText(block: IComponentDSLBuilder.() -> Unit) =
    onHoverShowText(buildComponent(block))

@ComponentDSL
fun IStyleBuilder.onHoverShowItem(item: HoverEvent.ItemStackInfo) = hoverEvent(HoverEvent.Action.SHOW_ITEM, item)
@ComponentDSL
fun IStyleBuilder.onHoverShowItem(item: ItemStack) = onHoverShowItem(HoverEvent.ItemStackInfo(item))
@ComponentDSL
fun IStyleBuilder.onHoverShowEntity(entity: HoverEvent.EntityTooltipInfo) =
    hoverEvent(HoverEvent.Action.SHOW_ENTITY, entity)

@ComponentDSL
fun IStyleBuilder.onHoverShowEntity(entityType: EntityType<*>, uuid: UUID, name: Component? = null) =
    onHoverShowEntity(HoverEvent.EntityTooltipInfo(entityType, uuid, name))
@ComponentDSL
fun IStyleBuilder.onHoverShowEntity(entityType: EntityType<*>, uuid: UUID, nameBlock: IComponentDSLBuilder.()->Unit) =
    onHoverShowEntity(entityType, uuid, buildComponent(nameBlock))
