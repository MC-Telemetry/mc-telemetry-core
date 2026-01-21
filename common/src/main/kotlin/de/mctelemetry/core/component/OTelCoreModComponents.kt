package de.mctelemetry.core.component

import com.mojang.serialization.Codec
import de.mctelemetry.core.OTelCoreMod
import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs

object OTelCoreModComponents {

    val DATA_COMPONENTS = DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.DATA_COMPONENT_TYPE)

    /**
     * When this component is added to an otherwise empty
     * [ObservationSourceContainerBlockEntity](de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity),
     * the block will generate
     * [ObservationSourceState](de.mctelemetry.core.observations.model.ObservationSourceState)s for each available
     * [IObservationSourceSingleton](de.mctelemetry.core.api.observations.IObservationSourceSingleton)
     * (respecting tags specifying which `IObservationSource`s are available).
     */
    val GENERATE_SINGLETON_STATES = DATA_COMPONENTS.register("generate_singleton_states") {
        DataComponentType.builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build()
    }

    fun init() {
        DATA_COMPONENTS.register()
    }
}
