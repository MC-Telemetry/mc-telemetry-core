package de.mctelemetry.core.api.metrics

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.observations.IObservationSource
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

@Suppress("MayBeConstant")
object OTelCoreModAPI {

    val MOD_ID = OTelCoreMod.MOD_ID


    val AttributeTypeMappings: ResourceKey<Registry<IMappedAttributeKeyType<*, *>>> = ResourceKey.createRegistryKey(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "attribute_type_mappings")
    )
    val ObservationSources: ResourceKey<Registry<IObservationSource<*, *>>> = ResourceKey.createRegistryKey(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "observation_sources")
    )

    object Limits {

        val INSTRUMENT_NAME_MAX_LENGTH = 256
        val INSTRUMENT_DESCRIPTION_MAX_LENGTH = 1024
        val INSTRUMENT_UNIT_MAX_LENGTH = 256
        val INSTRUMENT_ATTRIBUTES_MAX_COUNT = 64
        val INSTRUMENT_ATTRIBUTES_NAME_MAX_LENGTH = 256
    }
}
