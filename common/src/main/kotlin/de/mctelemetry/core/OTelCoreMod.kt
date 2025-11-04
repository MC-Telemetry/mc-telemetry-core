package de.mctelemetry.core

import com.mojang.serialization.Lifecycle
import de.mctelemetry.core.api.metrics.IMappedAttributeKeyType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.commands.scrape.CommandScrape
import de.mctelemetry.core.items.OTelCoreModItems
import de.mctelemetry.core.api.metrics.managar.IMetricsAccessor
import de.mctelemetry.core.commands.metrics.CommandMetrics
import de.mctelemetry.core.metrics.builtin.BuiltinInstruments
import de.mctelemetry.core.metrics.manager.InstrumentMetaManager
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.unaryPlus
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.Meter
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.WritableRegistry
import java.util.function.Supplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.message.SimpleMessageFactory
import java.util.Optional

object OTelCoreMod {

    const val MOD_ID = "mcotelcore"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    internal val meter: Meter by lazy {
        GlobalOpenTelemetry.getMeter("minecraft.mod.${MOD_ID}")
    }

    val TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)
    val OTEL_TAB: RegistrySupplier<CreativeModeTab>

    init {
        OTEL_TAB = TABS.register(
            "mcotelcore_tab",
            {
                CreativeTabRegistry.create(
                    Component.translatable("category.mcotelcore_tab"),
                    Supplier { ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK) }
                )
            }
        )
    }

    fun init() {
        TABS.register()

        OTelCoreModBlocks.init()
        OTelCoreModItems.init()

        debugMetrics()
        CommandRegistrationEvent.EVENT.register { evt, ctx, _ ->
            evt.root.addChild(buildCommand("mcotel") {
                +CommandScrape().command
                +CommandMetrics(ctx).command
            })
        }
        InstrumentMetaManager.register()
        BuiltinInstruments.register()
    }

    fun registerAttributeTypes(registry: WritableRegistry<IMappedAttributeKeyType<*, *>>?) {
        val attributeTypes: List<IMappedAttributeKeyType<*, *>> = NativeAttributeKeyTypes.ALL
        if (registry == null) {
            DeferredRegister.create(OTelCoreModAPI.MOD_ID, OTelCoreModAPI.AttributeTypeMappings).apply {
                for (nativeType in attributeTypes) {
                    register(nativeType.id) { nativeType }
                }
            }.register()
        } else {
            for (nativeType in attributeTypes) {
                registry.register(
                    ResourceKey.create(
                        OTelCoreModAPI.AttributeTypeMappings,
                        nativeType.id,
                    ),
                    nativeType,
                    RegistrationInfo(Optional.empty(), Lifecycle.stable())
                )
            }
        }
    }

    init {
        logger.debug {
            SimpleMessageFactory.INSTANCE.newMessage(
                "ClassLoader during mod class loading: {}",
                OTelCoreMod::class.java.classLoader
            )
        }
        val metricsAccessor = IMetricsAccessor.GLOBAL
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
        val accessor = IMetricsAccessor.GLOBAL
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
