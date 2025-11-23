package de.mctelemetry.core.neoforge

import com.mojang.brigadier.arguments.ArgumentType
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IMappedAttributeKeyType
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.commands.types.ArgumentTypes
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.observationrequest.ObservationRequestManagerClient
import de.mctelemetry.core.ui.OTelCoreModMenuTypes
import de.mctelemetry.core.ui.RedstoneScraperBlockScreen
import net.minecraft.Util
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.event.level.ChunkEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder
import thedarkcolour.kotlinforforge.neoforge.forge.DIST
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS


@Suppress("unused")
@Mod(OTelCoreMod.MOD_ID)
object OTelCoreModNeoForge {

    private fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>, I : ArgumentTypeInfo<A, T>>
            ArgumentTypes.PreparedArgumentTypeRegistration<A, T, I>.register(defReg: DeferredRegister<ArgumentTypeInfo<*, *>>) {
        ArgumentTypeInfos.registerByClass<A, T, I>(infoClass, info)
        defReg.register<I>(this.id.path) { -> info }
    }

    private fun createRegistries(event: NewRegistryEvent) {
        val attributeKeyRegistry =
            RegistryBuilder(OTelCoreModAPI.AttributeTypeMappings)
                .sync(true)
                .create() as WritableRegistry<IMappedAttributeKeyType<*, *>>
        OTelCoreMod.registerAttributeTypes(attributeKeyRegistry)
        event.register(attributeKeyRegistry)

        val observationSourceRegistry =
            RegistryBuilder(OTelCoreModAPI.ObservationSources)
                .sync(true)
                .create() as WritableRegistry<IObservationSource<*, *>>
        OTelCoreMod.registerObservationSources(observationSourceRegistry)
        event.register(observationSourceRegistry)
    }

    private fun registerCallbacks() {
        MOD_BUS.addListener(::createRegistries)
        if (DIST.isClient) {
            MOD_BUS.addListener { event: RegisterMenuScreensEvent ->
                event.register(
                    OTelCoreModMenuTypes.REDSTONE_SCRAPER_BLOCK.get(),
                    MenuScreens.ScreenConstructor(::RedstoneScraperBlockScreen)
                )
            }
            FORGE_BUS.addListener { event: ClientPlayerNetworkEvent.LoggingIn ->
                ObservationRequestManagerClient.onClientConnecting()
            }
            FORGE_BUS.addListener { event: ClientPlayerNetworkEvent.LoggingOut ->
                ObservationRequestManagerClient.onClientDisconnecting()
            }
        }
        FORGE_BUS.addListener(ChunkEvent.Load::class.java) { event ->
            if (event.chunk.highestGeneratedStatus.isOrAfter(ChunkStatus.FULL)) {
                event.chunk.findBlocks({ state ->
                                           state.hasBlockEntity() && state.block is ObservationSourceContainerBlock
                                       }) { pos, state ->

                    val entity = event.chunk.getBlockEntity(pos)
                    if (entity == null) {
                        OTelCoreMod.logger.trace("Null blockentity in ChunkEvent.Load at {}", pos)
                        return@findBlocks
                    }
                    if (entity !is ObservationSourceContainerBlockEntity) {
                        Util.logAndPauseIfInIde("Unexpected blockentity $entity in ChunkEvent.Load")
                        return@findBlocks
                    }
                    OTelCoreMod.logger.trace(
                        "Detected matching blockentity {} in ChunkEvent.Load, scheduling tick at {}",
                        entity,
                        pos
                    )
                    event.level.scheduleTick(pos, state.block, 1)
                }
            }
        }
    }

    private fun init() {
        registerCallbacks()
        OTelCoreMod.init()
        registerContent()
    }

    private fun registerContent() {
        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, OTelCoreMod.MOD_ID).let { defReg ->
            ArgumentTypes.register {
                it.register(defReg)
            }
            defReg.register(MOD_BUS)
        }
    }

    init {
        init()
    }
}
