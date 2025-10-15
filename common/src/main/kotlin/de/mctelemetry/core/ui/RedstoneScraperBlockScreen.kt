package de.mctelemetry.core.ui

import com.mojang.blaze3d.systems.RenderSystem
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.style
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractScrollWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory


class RedstoneScraperBlockScreen(menu: RedstoneScraperBlockMenu, playerInventory: Inventory, title: Component) :
    AbstractContainerScreen<RedstoneScraperBlockMenu>(menu, playerInventory, title) {

    override fun init() {
        imageWidth = 256
        imageHeight = 224

        inventoryLabelX = -10000
        inventoryLabelY = -10000

        super.init()

        val scrollWidget = ScrollWidget(leftPos + 10, topPos + 10, 150, 150, buildComponent { +"Test" })
        val rowWidget1 = RowWidget(10, 10, 130, 20, buildComponent { +"Test" })
        val rowWidget2 = RowWidget(10, 30, 130, 20, buildComponent { +"Test" })

        scrollWidget.addChildren(rowWidget1)
        scrollWidget.addChildren(rowWidget2)

        addRenderableWidget(scrollWidget)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, BACKGROUND)

        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

//        guiGraphics.drawString(font, buildComponent {
//            +"Redstone: "
//            append(menu.data.get(0).toString()) {
//                style {
//                    isBold = true
//                }
//            }
//        }, leftPos + 10, topPos + 50, 0x909090, false)

        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    companion object {
        private val BACKGROUND: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "textures/gui/redstone_scraper_block/redstone_scraper_block.png"
            )
    }

    private class ScrollWidget(leftPos: Int, topPos: Int, width: Int, height: Int, component: Component) :
        AbstractScrollWidget(leftPos, topPos, width, height, component) {

        private val children: MutableList<RowWidget> = mutableListOf()

        override fun getInnerHeight(): Int {
            return 1000
        }

        override fun scrollRate(): Double {
            return 1.0
        }

        fun addChildren(rowWidget: RowWidget) {
            children.add(rowWidget)
        }

        override fun renderContents(
            guiGraphics: GuiGraphics,
            i: Int,
            j: Int,
            f: Float
        ) {
            val pos = guiGraphics.pose()
            pos.pushPose()
            pos.translate(x.toDouble(), y.toDouble(), 0.0)

            children.forEach { it.render(guiGraphics, i, j, f) }

            pos.popPose()
        }

        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        }
    }

    private inner class RowWidget(leftPos: Int, topPos: Int, width: Int, height: Int, component: Component) :
        AbstractWidget(leftPos, topPos, width, height, component) {

        override fun renderWidget(
            guiGraphics: GuiGraphics,
            i: Int,
            j: Int,
            f: Float
        ) {
            guiGraphics.drawString(font, buildComponent {
                +"Redstone: "
                append(menu.data.get(0).toString()) {
                    style {
                        isBold = true
                    }
                }
            }, x, y, 0x909090, false)
        }

        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        }
    }
}