package de.mctelemetry.core.api.metrics

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import java.util.UUID

object BuiltinAttributeKeyTypes {

    val ALL: List<IMappedAttributeKeyType<*, *>> = listOf(
        BlockPosType,
        GlobalPosType,
        ItemType,
        DirectionType,
        UUIDType,
    )

    object BlockPosType : IMappedAttributeKeyType<BlockPos, List<Long>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "block_position")
        )

        override val baseType: GenericAttributeType<List<Long>> = GenericAttributeType.LONG_ARRAY
        override val valueType: Class<BlockPos> = BlockPos::class.java

        override fun format(value: BlockPos): List<Long> {
            return listOf(value.x.toLong(), value.y.toLong(), value.z.toLong())
        }

        fun format(x: Long, y: Long, z: Long): List<Long> {
            return listOf(x, y, z)
        }

        override fun canConvertDirectlyTo(supertype: IMappedAttributeKeyType<*, *>): Boolean {
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> true
                else -> false
            }
        }

        override fun <R : Any> convertDirectlyTo(supertype: IMappedAttributeKeyType<R, *>, value: BlockPos): R? {
            @Suppress("UNCHECKED_CAST") // known values from when-matching
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> "${value.x},${value.y},${value.z}" as R
                NativeAttributeKeyTypes.StringArrayType -> listOf(
                    value.x.toString(),
                    value.y.toString(),
                    value.z.toString()
                ) as R
                else -> null
            }
        }
    }

    object GlobalPosType : IMappedAttributeKeyType<GlobalPos, List<String>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "global_position")
        )

        override val baseType: GenericAttributeType<List<String>> = GenericAttributeType.STRING_ARRAY
        override val valueType: Class<GlobalPos> = GlobalPos::class.java

        override fun format(value: GlobalPos): List<String> {
            val pos = value.pos
            return listOf(value.dimension.location().toString(), pos.x.toString(), pos.y.toString(), pos.z.toString())
        }

        fun format(dimension: ResourceKey<Level>, pos: BlockPos): List<String> {
            return listOf(dimension.location().toString(), pos.x.toString(), pos.y.toString(), pos.z.toString())
        }

        fun format(dimension: ResourceKey<Level>, x: Long, y: Long, z: Long): List<String> {
            return listOf(dimension.location().toString(), x.toString(), y.toString(), z.toString())
        }

        override fun canConvertDirectlyTo(supertype: IMappedAttributeKeyType<*, *>): Boolean {
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> true
                BlockPosType -> true
                else -> false
            }
        }

        override fun <R : Any> convertDirectlyTo(supertype: IMappedAttributeKeyType<R, *>, value: GlobalPos): R? {
            @Suppress("UNCHECKED_CAST") // known values from when-matching
            return when (supertype) {
                NativeAttributeKeyTypes.StringType -> "${value.dimension.location()}@${value.pos.x},${value.pos.y},${value.pos.z}" as R
                BlockPosType -> value.pos as R
                else -> null
            }
        }
    }

    object DirectionType : IMappedAttributeKeyType<Direction, String> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "direction")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueType: Class<Direction> = Direction::class.java

        override fun format(value: Direction): String {
            return value.toString()
        }
    }

    object ItemType : IMappedAttributeKeyType<Item, String> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "item")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueType: Class<Item> = Item::class.java

        override fun format(value: Item): String {
            val key = value.`arch$holder`().unwrapKey().orElseThrow()
            return key.location().toString()
        }
    }

    object UUIDType : IMappedAttributeKeyType<UUID, String> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "uuid")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueType: Class<UUID> = UUID::class.java


        override fun format(value: UUID): String {
            return value.toString()
        }
    }
}
