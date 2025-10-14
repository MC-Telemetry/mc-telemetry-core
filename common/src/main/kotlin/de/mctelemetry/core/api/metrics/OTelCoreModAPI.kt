package de.mctelemetry.core.api.metrics

import de.mctelemetry.core.OTelCoreMod
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

@Suppress("MayBeConstant")
object OTelCoreModAPI {
    val MOD_ID = OTelCoreMod.MOD_ID

    val AttributeTypeMappings: ResourceKey<Registry<IMappedAttributeKeyType<*,*>>> = ResourceKey.createRegistryKey(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "attribute_type_mappings")
    )
}
