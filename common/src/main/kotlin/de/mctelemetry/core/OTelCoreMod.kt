package de.mctelemetry.core

import com.mojang.serialization.Lifecycle
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes
import de.mctelemetry.core.commands.metrics.CommandMetrics
import de.mctelemetry.core.commands.scrape.CommandScrape
import de.mctelemetry.core.instruments.builtin.BuiltinInstruments
import de.mctelemetry.core.instruments.manager.server.ServerInstrumentMetaManager
import de.mctelemetry.core.items.OTelCoreModItems
import de.mctelemetry.core.network.instrumentsync.*
import de.mctelemetry.core.network.observations.container.observationrequest.C2SObservationsRequestPayload
import de.mctelemetry.core.network.observations.container.observationrequest.ObservationRequestManagerServer
import de.mctelemetry.core.network.observations.container.observationrequest.S2CObservationsPayload
import de.mctelemetry.core.network.observations.container.settings.C2SObservationSourceSettingsUpdatePayload
import de.mctelemetry.core.observations.ObservationSources
import de.mctelemetry.core.ui.components.SuggestingTextBoxComponent
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.unaryPlus
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.platform.Platform
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import dev.architectury.utils.Env
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.Meter
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*

object OTelCoreMod {

    const val MOD_ID = "mcotelcore"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    internal val meter: Meter by lazy {
        GlobalOpenTelemetry.getMeter("minecraft.mod.${MOD_ID}")
    }

    val TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)
    val OTEL_TAB: RegistrySupplier<CreativeModeTab> = TABS.register("mcotelcore_tab")
    {
        CreativeTabRegistry.create(
            Component.translatable("category.mcotelcore_tab")
        ) { ItemStack(OTelCoreModItems.REDSTONE_SCRAPER_BLOCK) }
    }


    fun registerCallbacks() {
        ServerInstrumentMetaManager.register()
        BuiltinInstruments.register()
        CommandRegistrationEvent.EVENT.register { evt, ctx, _ ->
            evt.root.addChild(buildCommand("mcotel") {
                +CommandScrape().command
                +CommandMetrics(ctx).command
            })
        }
        ObservationSourceContainerBlock.RightClickBlockListener.register()
        A2AInstrumentAddedPayload.register()
        A2AInstrumentRemovedPayload.register()
        C2SAllInstrumentRequestPayload.register()
        S2CAllInstrumentsPayload.register()
        S2CReservedNameAddedPayload.register()
        S2CReservedNameRemovedPayload.register()
        ObservationRequestManagerServer.registerListeners()
        S2CObservationsPayload.register()
        C2SObservationsRequestPayload.register()
        C2SObservationSourceSettingsUpdatePayload.register()
        SyncSubscriptions.register()

        if (Platform.getEnvironment() == Env.CLIENT) {
            KeyBindingManager.register()
        }
    }

    fun registerContent() {
        TABS.register()

        if (Platform.getEnvironment() == Env.CLIENT) {
            SuggestingTextBoxComponent.register()
        }

        OTelCoreModBlocks.init()
        OTelCoreModBlockEntityTypes.init()
        OTelCoreModItems.init()
    }

    fun init() {
        registerCallbacks()
        registerContent()
    }

    fun registerAttributeTypes(registry: WritableRegistry<IAttributeKeyTypeTemplate<*, *>>?) {
        val attributeTypes: List<IAttributeKeyTypeTemplate<*, *>> =
            NativeAttributeKeyTypes.ALL + BuiltinAttributeKeyTypes.ALL
        if (registry == null) {
            DeferredRegister.create(OTelCoreModAPI.MOD_ID, OTelCoreModAPI.AttributeTypeMappings).apply {
                for (nativeType in attributeTypes) {
                    register(nativeType.id.location()) { nativeType }
                }
            }.register()
        } else {
            for (nativeType in attributeTypes) {
                registry.register(
                    nativeType.id,
                    nativeType,
                    RegistrationInfo(Optional.empty(), Lifecycle.stable())
                )
            }
        }
    }

    fun registerObservationSources(registry: WritableRegistry<IObservationSource<*, *>>?) {
        val observationSources: List<IObservationSource<*, *>> = ObservationSources.ALL
        if (registry == null) {
            DeferredRegister.create(OTelCoreModAPI.MOD_ID, OTelCoreModAPI.ObservationSources).apply {
                for (observationSource in observationSources) {
                    register(observationSource.id.location()) { observationSource }
                }
            }.register()
        } else {
            for (observationSource in observationSources) {
                registry.register(
                    observationSource.id,
                    observationSource,
                    RegistrationInfo(Optional.empty(), Lifecycle.stable())
                )
            }
        }
    }
}
