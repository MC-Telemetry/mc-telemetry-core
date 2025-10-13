package de.mctelemetry.core.ui

import com.mojang.blaze3d.systems.RenderSystem
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.style
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory


class RedstoneScraperBlockScreen(menu: RedstoneScraperBlockMenu, playerInventory: Inventory, title: Component) :
    AbstractContainerScreen<RedstoneScraperBlockMenu>(menu, playerInventory, title) {

    override fun init() {
        super.init()

        val editBox = EditBox(font, leftPos - 100, topPos + 10, 150, 20, buildComponent { +"EditBox" })
        addRenderableWidget(editBox)
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

        val y1 = ((height - imageHeight) / 2) + 12
        val y2 = ((height - imageHeight) / 2) + 24

        guiGraphics.drawString(font, "Stored Water: " + this.menu.data.get(0), width / 2, y1, 0x909090, false)
        guiGraphics.drawString(font, buildComponent {
            +"Redstone: "
            append(menu.data.get(1).toString()) {
                style {
                    isBold = true
                }
            }
        }, width / 2, y2, 0x909090, false)

        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    companion object {
        private val BACKGROUND: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "textures/gui/redstone_scraper_block/redstone_scraper_block.png")
    }
}