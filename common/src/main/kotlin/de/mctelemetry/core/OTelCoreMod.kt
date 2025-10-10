package de.mctelemetry.core

import de.mctelemetry.core.commands.scrape.CommandScrape
import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import java.util.function.Supplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.*
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.message.SimpleMessageFactory

object OTelCoreMod {

    const val MOD_ID = "mcotelcore"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(MOD_ID, Registries.BLOCK)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(MOD_ID, Registries.ITEM);

    val OTEL_TAB: RegistrySupplier<CreativeModeTab>

    val RUBY_BLOCK: RegistrySupplier<Block> = registerBlock("ruby_block", {
        Block(BlockBehaviour.Properties.of())
    })
    val RUBY: RegistrySupplier<Item> = registerItem("ruby_block", {
        BlockItem(RUBY_BLOCK.get(), Item.Properties().`arch$tab`(OTEL_TAB))
    })

    init {
        OTEL_TAB = TABS.register(
            "mcotelcore_tab",
            {
                CreativeTabRegistry.create(
                    Component.translatable("category.mcotelcore_tab"),
                    Supplier { ItemStack(RUBY) }
                )
            }
        )
    }

    fun init() {
        TABS.register()
        BLOCKS.register()
        ITEMS.register()

        debugMetrics()
        CommandRegistrationEvent.EVENT.register { evt, a, b ->
            evt.root.addChild(buildCommand("mcotel") {
                then(CommandScrape().command)
            })
        }
    }

    fun registerItem(name: String, item: Supplier<Item>): RegistrySupplier<Item> {
        return ITEMS.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
    }

    fun registerBlock(name: String, block: Supplier<Block?>?): RegistrySupplier<Block> {
        return BLOCKS.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), block)
    }

    init {
        logger.debug {
            SimpleMessageFactory.INSTANCE.newMessage(
                "ClassLoader during mod class loading: {}",
                OTelCoreMod::class.java.classLoader
            )
        }
        val metricsAccessor = MetricsAccessor.INSTANCE
        logger.debug("MetricsAccessor during mod class loading: {}", metricsAccessor)
        if (metricsAccessor != null) {
            logger.info("Performing initial metrics collection, may throw errors which can be safely ignored (probably?)")
            metricsAccessor.collect()
            logger.info("Initial metric collection done, any errors following this should be treated as errors again")
        } else {
            logger.debug("Could not find MetricsAccessor during Mod-Init.")
        }
    }

    private fun debugMetrics() {
        val accessor = MetricsAccessor.INSTANCE
        if (accessor != null) {
            logger.info("Metrics: ")
            accessor.collect().forEach { (k, v) ->
                logger.info(
                    """
                    |$k:
                    |   TYPE: ${v.type}
                    |   UNIT: ${v.unit}
                    |   DESCRIPTION: ${v.description}
                    |   DATA: ${v.data}
                    """.trimIndent().replaceIndentByMargin("    ")
                )
            }
        } else {
            logger.info("Metrics not configured")
        }
    }
}
